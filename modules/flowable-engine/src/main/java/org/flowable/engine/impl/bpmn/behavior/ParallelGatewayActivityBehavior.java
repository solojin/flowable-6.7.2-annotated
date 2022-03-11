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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.ParallelGateway;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 并行网关活动行为类
 * 实现并行网关和BPMN 2.0规范中定义的网关。
 * 
 * 并行网关可用于将一条执行路径拆分为多条执行路径（以及拆分/分叉行为），每个传出序列流一条。
 * 
 * 并行网关还可用于合并或连接执行路径（和连接）。在这种情况下，在每一个传入的序列流上，执行都需要到达，然后才能离开
 * 并行网关（如果有多个传出序列流，则可能执行fork行为）。
 * 
 * 请注意，与规范（第436页）略有不同：“如果每个传入序列流上至少有一个令牌，那么并行网关将被激活。”我们只检查传入数据的数量
 * 标记到sequenceflow的数量。因此，如果两个令牌通过相同的序列流到达，我们的实现将激活网关。
 * 
 * 请注意，具有一个传入和多个传出序列流的并行网关，与在给定活动上具有多个传出序列流相同。然而，并行网关却不能
 * 检查输出序列流的条件。
 * 
 * @author Joram Barrez
 * @author Tom Baeyens
 */
public class ParallelGatewayActivityBehavior extends GatewayActivityBehavior {

    private static final long serialVersionUID = 1840892471343975524L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelGatewayActivityBehavior.class);

    @Override
    public void execute(DelegateExecution execution) {

        // 首先，停止执行
        execution.inactivate();

        // 连接
        FlowElement flowElement = execution.getCurrentFlowElement();
        ParallelGateway parallelGateway = null;
        if (flowElement instanceof ParallelGateway) {
            parallelGateway = (ParallelGateway) flowElement;
        } else {
            throw new FlowableException("Programmatic error: parallel gateway behaviour can only be applied" + " to a ParallelGateway instance, but got an instance of " + flowElement);
        }

        lockFirstParentScope(execution);

        DelegateExecution multiInstanceExecution = null;
        if (hasMultiInstanceParent(parallelGateway)) {
            multiInstanceExecution = findMultiInstanceParentExecution(execution);
        }

        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager();
        Collection<ExecutionEntity> joinedExecutions = executionEntityManager.findInactiveExecutionsByActivityIdAndProcessInstanceId(execution.getCurrentActivityId(), execution.getProcessInstanceId());
        if (multiInstanceExecution != null) {
            joinedExecutions = cleanJoinedExecutions(joinedExecutions, multiInstanceExecution);
        }

        int nbrOfExecutionsToJoin = parallelGateway.getIncomingFlows().size();
        int nbrOfExecutionsCurrentlyJoined = joinedExecutions.size();

        // 分叉

        // 需要设置所有历史活动联接的结束时间
        CommandContextUtil.getActivityInstanceEntityManager().recordActivityEnd((ExecutionEntity) execution, null);

        if (nbrOfExecutionsCurrentlyJoined == nbrOfExecutionsToJoin) {

            // 分叉
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("parallel gateway '{}' ({}) activates: {} of {} joined", execution.getCurrentActivityId(), 
                        execution.getId(), nbrOfExecutionsCurrentlyJoined, nbrOfExecutionsToJoin);
            }

            if (parallelGateway.getIncomingFlows().size() > 1) {

                // 所有（现在处于非活动状态）子项都将被删除。
                for (ExecutionEntity joinedExecution : joinedExecutions) {

                    // 当前执行将被重用，而不会被删除
                    if (!joinedExecution.getId().equals(execution.getId())) {
                        executionEntityManager.deleteRelatedDataForExecution(joinedExecution, null, false);
                        executionEntityManager.delete(joinedExecution);
                    }

                }
            }

            // TODO:这里的潜在优化：重用超过1次执行，当前仅1次
            // false->忽略并行网关上的条件
            CommandContextUtil.getAgenda().planTakeOutgoingSequenceFlowsOperation((ExecutionEntity) execution, false);

        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("parallel gateway '{}' ({}) does not activate: {} of {} joined", execution.getCurrentActivityId(), 
                    execution.getId(), nbrOfExecutionsCurrentlyJoined, nbrOfExecutionsToJoin);
        }

    }

    protected Collection<ExecutionEntity> cleanJoinedExecutions(Collection<ExecutionEntity> joinedExecutions, DelegateExecution multiInstanceExecution) {
        List<ExecutionEntity> cleanedExecutions = new ArrayList<>();
        for (ExecutionEntity executionEntity : joinedExecutions) {
            if (isChildOfMultiInstanceExecution(executionEntity, multiInstanceExecution)) {
                cleanedExecutions.add(executionEntity);
            }
        }
        return cleanedExecutions;
    }

    protected boolean isChildOfMultiInstanceExecution(DelegateExecution executionEntity, DelegateExecution multiInstanceExecution) {
        boolean isChild = false;
        DelegateExecution parentExecution = executionEntity.getParent();
        if (parentExecution != null) {
            if (parentExecution.getId().equals(multiInstanceExecution.getId())) {
                isChild = true;
            } else {
                boolean isNestedChild = isChildOfMultiInstanceExecution(parentExecution, multiInstanceExecution);
                if (isNestedChild) {
                    isChild = true;
                }
            }
        }

        return isChild;
    }

    protected boolean hasMultiInstanceParent(FlowNode flowNode) {
        boolean hasMultiInstanceParent = false;
        if (flowNode.getSubProcess() != null) {
            if (flowNode.getSubProcess().getLoopCharacteristics() != null) {
                hasMultiInstanceParent = true;
            } else {
                boolean hasNestedMultiInstanceParent = hasMultiInstanceParent(flowNode.getSubProcess());
                if (hasNestedMultiInstanceParent) {
                    hasMultiInstanceParent = true;
                }
            }
        }

        return hasMultiInstanceParent;
    }

    protected DelegateExecution findMultiInstanceParentExecution(DelegateExecution execution) {
        DelegateExecution multiInstanceExecution = null;
        DelegateExecution parentExecution = execution.getParent();
        if (parentExecution != null && parentExecution.getCurrentFlowElement() != null) {
            FlowElement flowElement = parentExecution.getCurrentFlowElement();
            if (flowElement instanceof Activity) {
                Activity activity = (Activity) flowElement;
                if (activity.getLoopCharacteristics() != null) {
                    multiInstanceExecution = parentExecution;
                }
            }

            if (multiInstanceExecution == null) {
                DelegateExecution potentialMultiInstanceExecution = findMultiInstanceParentExecution(parentExecution);
                if (potentialMultiInstanceExecution != null) {
                    multiInstanceExecution = potentialMultiInstanceExecution;
                }
            }
        }

        return multiInstanceExecution;
    }

}
