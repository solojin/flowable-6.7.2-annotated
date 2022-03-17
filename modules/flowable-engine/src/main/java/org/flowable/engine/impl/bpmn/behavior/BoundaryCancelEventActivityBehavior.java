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

import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.BoundaryEvent;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.history.DeleteReason;
import org.flowable.engine.impl.bpmn.helper.ScopeUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.CompensateEventSubscriptionEntity;

/**
 * 边界取消事件活动行为类
 *
 * 取消边界事件（cancel boundary event），在事务取消时触发。当取消边界事件触发时，首先会中断当前范围的所有活动执行。
 * 接下来，启动事务范围内所有有效的的补偿边界事件（compensation boundary event）。
 * 补偿会同步执行，也就是说在离开事务前，边界事件会等待补偿完成。当补偿完成时，沿取消边界事件的任何出口顺序流离开事务子流程。
 *
 * 请注意：一个事务子流程只允许使用一个取消边界事件。
 *
 * 请注意：如果事务子流程中有嵌套的子流程，只会对成功完成的子流程触发补偿。
 *
 * 请注意：如果取消边界事件放置在具有多实例特性的事务子流程上，如果一个实例触发了取消，则边界事件将取消所有实例。
 *
 * @author Tijs Rademakers
 */
public class BoundaryCancelEventActivityBehavior extends BoundaryEventActivityBehavior {

    private static final long serialVersionUID = 1L;

    @Override
    public void trigger(DelegateExecution execution, String triggerName, Object triggerData) {
        BoundaryEvent boundaryEvent = (BoundaryEvent) execution.getCurrentFlowElement();

        CommandContext commandContext = Context.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();

        ExecutionEntity subProcessExecution = null;
        // TODO:这是可以优化的。不需要对所有执行情况进行全面搜索
        List<ExecutionEntity> processInstanceExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(execution.getProcessInstanceId());
        for (ExecutionEntity childExecution : processInstanceExecutions) {
            if (childExecution.getCurrentFlowElement() != null
                    && childExecution.getCurrentFlowElement().getId().equals(boundaryEvent.getAttachedToRefId())) {
                subProcessExecution = childExecution;
                break;
            }
        }

        if (subProcessExecution == null) {
            throw new FlowableException("No execution found for sub process of boundary cancel event " + boundaryEvent.getId());
        }

        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();
        List<CompensateEventSubscriptionEntity> eventSubscriptions = eventSubscriptionService.findCompensateEventSubscriptionsByExecutionId(subProcessExecution.getParentId());

        if (!eventSubscriptions.isEmpty()) {

            String deleteReason = DeleteReason.BOUNDARY_EVENT_INTERRUPTING + "(" + boundaryEvent.getId() + ")";

            // 取消边界总是同步的
            ScopeUtil.throwCompensationEvent(eventSubscriptions, execution, false);
            executionEntityManager.deleteExecutionAndRelatedData(subProcessExecution, deleteReason, false);
            if (subProcessExecution.getCurrentFlowElement() instanceof Activity) {
                Activity activity = (Activity) subProcessExecution.getCurrentFlowElement();
                if (activity.getLoopCharacteristics() != null) {
                    ExecutionEntity miExecution = subProcessExecution.getParent();
                    List<ExecutionEntity> miChildExecutions = executionEntityManager.findChildExecutionsByParentExecutionId(miExecution.getId());
                    for (ExecutionEntity miChildExecution : miChildExecutions) {
                        if (!subProcessExecution.getId().equals(miChildExecution.getId()) && activity.getId().equals(miChildExecution.getCurrentActivityId())) {
                            executionEntityManager.deleteExecutionAndRelatedData(miChildExecution, deleteReason, false);
                        }
                    }
                }
            }
        }
        leave(execution);
    }
}
