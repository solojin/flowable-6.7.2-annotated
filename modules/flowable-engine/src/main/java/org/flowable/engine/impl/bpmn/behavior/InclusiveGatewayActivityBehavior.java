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

import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

import org.flowable.bpmn.model.FlowNode;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.delegate.InactiveActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 包容网关活动行为类
 * Implementation of the Inclusive Gateway/OR gateway/inclusive data-based gateway as defined in the BPMN specification.
 *
 * @author Tijs Rademakers
 * @author Tom Van Buskirk
 * @author Joram Barrez
 */
public class InclusiveGatewayActivityBehavior extends GatewayActivityBehavior implements InactiveActivityBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(InclusiveGatewayActivityBehavior.class.getName());

    @Override
    public void execute(DelegateExecution execution) {
        // 加入包容网关的工作如下：
        // 当执行进入时，它将被停止。
        //
        // 所有未激活的执行都保留在包容性网关中，直到所有可以到达包容性网关的执行都到达它。
        //
        // 在执行更改时重复此检查，直到未激活执行离开网关。

        execution.inactivate();
        executeInclusiveGatewayLogic((ExecutionEntity) execution, false);
    }

    @Override
    public void executeInactive(ExecutionEntity executionEntity) {
        executeInclusiveGatewayLogic(executionEntity, true);
    }

    protected void executeInclusiveGatewayLogic(ExecutionEntity execution, boolean inactiveCheck) {
        CommandContext commandContext = Context.getCommandContext();
        ExecutionEntityManager executionEntityManager = CommandContextUtil.getExecutionEntityManager(commandContext);

        lockFirstParentScope(execution);

        Collection<ExecutionEntity> allExecutions = executionEntityManager.findChildExecutionsByProcessInstanceId(execution.getProcessInstanceId());
        Iterator<ExecutionEntity> executionIterator = allExecutions.iterator();
        boolean oneExecutionCanReachGatewayInstance = false;
        while (!oneExecutionCanReachGatewayInstance && executionIterator.hasNext()) {
            ExecutionEntity executionEntity = executionIterator.next();
            if (!executionEntity.getActivityId().equals(execution.getCurrentActivityId())) {
                if (ExecutionGraphUtil.isReachable(execution.getProcessDefinitionId(), executionEntity.getActivityId(), execution.getCurrentActivityId())) {
                    // 在相同的执行路径中检查
                    if (executionEntity.getParentId().equals(execution.getParentId())) {
                        oneExecutionCanReachGatewayInstance = true;
                        break;
                    }
                }
            } else if (executionEntity.isActive() && (executionEntity.getId().equals(execution.getId()) || isAsynchronousActivity(executionEntity))) {
                // 特殊情况：执行已到达包容性网关，但尚未执行该执行的操作
                oneExecutionCanReachGatewayInstance = true;
                break;
            }
        }

        // 需要设置所有历史活动联接的结束时间
        if (!inactiveCheck || !oneExecutionCanReachGatewayInstance) {
            CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityEnd(execution, null);
        }

        // 如果没有执行可以到达网关，网关将激活并执行fork行为
        if (!oneExecutionCanReachGatewayInstance) {

            LOGGER.debug("Inclusive gateway cannot be reached by any execution and is activated");

            // 在此处杀死所有执行（传入的执行除外）
            Collection<ExecutionEntity> executionsInGateway = executionEntityManager
                .findInactiveExecutionsByActivityIdAndProcessInstanceId(execution.getCurrentActivityId(), execution.getProcessInstanceId());
            for (ExecutionEntity executionEntityInGateway : executionsInGateway) {
                if (!executionEntityInGateway.getId().equals(execution.getId()) && executionEntityInGateway.getParentId().equals(execution.getParentId())) {

                    if (!Objects.equals(executionEntityInGateway.getActivityId(), execution.getActivityId())) {
                        CommandContextUtil.getActivityInstanceEntityManager(commandContext).recordActivityEnd(executionEntityInGateway, null);
                    }

                    executionEntityManager.deleteExecutionAndRelatedData(executionEntityInGateway, null, false);
                }
            }

            // 离开
            CommandContextUtil.getAgenda(commandContext).planTakeOutgoingSequenceFlowsOperation(execution, true);
        }
    }

    protected boolean isAsynchronousActivity(ExecutionEntity executionEntity) {
        return executionEntity.getCurrentFlowElement() instanceof FlowNode && ((FlowNode) executionEntity.getCurrentFlowElement()).isAsynchronous();
    }
}
