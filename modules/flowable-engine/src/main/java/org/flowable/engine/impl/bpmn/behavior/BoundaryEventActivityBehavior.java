/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.engine.impl.bpmn.behavior;

import java.util.Collections;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.delegate.InterruptibleActivityBehaviour;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;

/**
 * 边界事件活动行为类
 *
 * @author Joram Barrez
 */
public class BoundaryEventActivityBehavior extends FlowNodeActivityBehavior {

    private static final long serialVersionUID = 1L;

    protected boolean interrupting;

    public BoundaryEventActivityBehavior() {
    }

    public BoundaryEventActivityBehavior(boolean interrupting) {
        this.interrupting = interrupting;
    }

    @Override
    public void execute(DelegateExecution execution) {
        // 被子类覆盖
    }

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        ExecutionEntity executionEntity = (ExecutionEntity) execution;

        CommandContext commandContext = Context.getCommandContext();

        if (interrupting) {
            executeInterruptingBehavior(executionEntity, commandContext);
        } else {
            executeNonInterruptingBehavior(executionEntity, commandContext);
        }
    }

    protected void executeInterruptingBehavior(ExecutionEntity executionEntity, CommandContext commandContext) {
        // 销毁作用域操作将查找父执行和
        // 销毁整个作用域，并使用此父执行保留边界事件。
        //
        // 另一方面，下面的take outgoing seq flows操作（非中断else子句）使用
        // 要离开的子执行，这使作用域保持活动状态。
        // 这就是我们需要的。

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        ExecutionEntity attachedRefScopeExecution = executionEntityManager.findById(executionEntity.getParentId());

        ExecutionEntity parentScopeExecution = null;
        ExecutionEntity currentlyExaminedExecution = executionEntityManager.findById(attachedRefScopeExecution.getParentId());
        while (currentlyExaminedExecution != null && parentScopeExecution == null) {
            if (currentlyExaminedExecution.isScope()) {
                parentScopeExecution = currentlyExaminedExecution;
            } else {
                currentlyExaminedExecution = executionEntityManager.findById(currentlyExaminedExecution.getParentId());
            }
        }

        if (parentScopeExecution == null) {
            throw new FlowableException("Programmatic error: no parent scope execution found for boundary event");
        }

        if (attachedRefScopeExecution.getCurrentFlowElement() instanceof FlowNode) {
            Object behavior = ((FlowNode) attachedRefScopeExecution.getCurrentFlowElement()).getBehavior();
            if (behavior instanceof InterruptibleActivityBehaviour) {
                ((InterruptibleActivityBehaviour) behavior).interrupted(attachedRefScopeExecution);
            }
        }

        deleteChildExecutions(attachedRefScopeExecution, executionEntity, commandContext);

        // 为边界事件执行设置新父级
        executionEntity.setParent(parentScopeExecution);

        // 当边界事件没有传出序列流时，TakeOutgoingSequenceFlow将不会设置正确的历史记录
        //（这是一个理论案例……不应使用没有传出序列流的边界事件…）
        if (executionEntity.getCurrentFlowElement() instanceof FlowNode
                && ((FlowNode) executionEntity.getCurrentFlowElement()).getOutgoingFlows().isEmpty()) {
            
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityEnd(executionEntity, null);
        }

        CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(executionEntity, true);
    }

    protected void executeNonInterruptingBehavior(ExecutionEntity executionEntity, CommandContext commandContext) {
        // 无中断：当前执行被赋予第一个父级
        // 范围（不是其直接父级）
        //
        // 为什么？因为这次执行与当前父级执行（边界事件处于启用状态的执行）：当它被删除或其他任何情况时，
        // 这根本不会影响新的执行，在这方面它是完全独立的。

        // 注意：如果父对象的父对象不存在，这将成为流程实例中的并发执行！

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        ExecutionEntity parentExecutionEntity = executionEntityManager.findById(executionEntity.getParentId());

        ExecutionEntity scopeExecution = null;
        ExecutionEntity currentlyExaminedExecution = executionEntityManager.findById(parentExecutionEntity.getParentId());
        while (currentlyExaminedExecution != null && scopeExecution == null) {
            if (currentlyExaminedExecution.isScope()) {
                scopeExecution = currentlyExaminedExecution;
            } else {
                currentlyExaminedExecution = executionEntityManager.findById(currentlyExaminedExecution.getParentId());
            }
        }

        if (scopeExecution == null) {
            throw new FlowableException("Programmatic error: no parent scope execution found for boundary event");
        }

        CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityEnd(executionEntity, null);

        ExecutionEntity nonInterruptingExecution = executionEntityManager.createChildExecution(scopeExecution);
        nonInterruptingExecution.setActive(false);
        nonInterruptingExecution.setCurrentFlowElement(executionEntity.getCurrentFlowElement());
        
        // 为非中断边界事件创建新的开始活动实例
        CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityStart(executionEntity);

        CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(nonInterruptingExecution, true);
    }

    protected void deleteChildExecutions(ExecutionEntity parentExecution, ExecutionEntity outgoingExecutionEntity, CommandContext commandContext) {
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);
        String deleteReason = DeleteReason.BOUNDARY_EVENT_INTERRUPTING + " (" + outgoingExecutionEntity.getCurrentActivityId() + ")";
        executionEntityManager.deleteChildExecutions(parentExecution, Collections.singletonList(outgoingExecutionEntity.getId()), null,
                deleteReason, true, outgoingExecutionEntity.getCurrentFlowElement());

        executionEntityManager.deleteExecutionAndRelatedData(parentExecution, deleteReason, false, false, true, outgoingExecutionEntity.getCurrentFlowElement());
    }

    public boolean isInterrupting() {
        return interrupting;
    }

    public void setInterrupting(boolean interrupting) {
        this.interrupting = interrupting;
    }

}
