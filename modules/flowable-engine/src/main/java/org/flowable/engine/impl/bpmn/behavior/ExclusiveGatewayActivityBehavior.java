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

import java.util.Iterator;

import org.flowable.bpmn.model.ExclusiveGateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.impl.FlowableEventBuilder;
import org.flowable.engine.impl.bpmn.helper.SkipExpressionUtil;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.condition.ConditionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 独占网关活动行为类
 * 实现BPMN规范中定义的独占网关/XOR网关/基于数据的独占网关。
 * 
 * @author Joram Barrez
 */
public class ExclusiveGatewayActivityBehavior extends GatewayActivityBehavior {

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(ExclusiveGatewayActivityBehavior.class);

    /**
     * BPMN的默认行为，即获取每个流出序列流（条件的计算结果为true），对独占网关无效。
     * 
     * 因此，该行为被覆盖，并被正确的行为所取代：选择条件评估为true（或没有条件）的第一个序列流，并离开
     * 通过该序列流的活动。
     * 
     * 如果未选择序列流（即所有条件评估为false），则采用默认序列流（如果定义）。
     */
    @Override
    public void leave(DelegateExecution execution) {

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Leaving exclusive gateway '{}'", execution.getCurrentActivityId());
        }

        ExclusiveGateway exclusiveGateway = (ExclusiveGateway) execution.getCurrentFlowElement();

        CommandContext commandContext = CommandContextUtil.getCommandContext();
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        FlowableEventDispatcher eventDispatcher = null;
        if (processEngineConfiguration != null) {
            eventDispatcher = processEngineConfiguration.getEventDispatcher();
        }
        if (eventDispatcher != null && eventDispatcher.isEnabled()) {
            processEngineConfiguration.getEventDispatcher().dispatchEvent(
                    FlowableEventBuilder.createActivityEvent(FlowableEngineEventType.ACTIVITY_COMPLETED, exclusiveGateway.getId(), exclusiveGateway.getName(), execution.getId(),
                            execution.getProcessInstanceId(), execution.getProcessDefinitionId(), exclusiveGateway),
                    processEngineConfiguration.getEngineCfgKey());
        }

        SequenceFlow outgoingSequenceFlow = null;
        SequenceFlow defaultSequenceFlow = null;
        String defaultSequenceFlowId = exclusiveGateway.getDefaultFlow();

        // 确定要采取的顺序流
        Iterator<SequenceFlow> sequenceFlowIterator = exclusiveGateway.getOutgoingFlows().iterator();
        while (outgoingSequenceFlow == null && sequenceFlowIterator.hasNext()) {
            SequenceFlow sequenceFlow = sequenceFlowIterator.next();

            String skipExpressionString = sequenceFlow.getSkipExpression();
            if (!SkipExpressionUtil.isSkipExpressionEnabled(skipExpressionString, sequenceFlow.getId(), execution, commandContext)) {
                boolean conditionEvaluatesToTrue = ConditionUtil.hasTrueCondition(sequenceFlow, execution);
                if (conditionEvaluatesToTrue && (defaultSequenceFlowId == null || !defaultSequenceFlowId.equals(sequenceFlow.getId()))) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Sequence flow '{}' selected as outgoing sequence flow.", sequenceFlow.getId());
                    }
                    outgoingSequenceFlow = sequenceFlow;
                }
                
            } else if (SkipExpressionUtil.shouldSkipFlowElement(skipExpressionString, sequenceFlow.getId(), execution, Context.getCommandContext())) {
                outgoingSequenceFlow = sequenceFlow;
            }

            // 已经储存好了，如果我们以后需要的话。为循环保存一个。
            if (defaultSequenceFlowId != null && defaultSequenceFlowId.equals(sequenceFlow.getId())) {
                defaultSequenceFlow = sequenceFlow;
            }

        }

        // 离开网关
        if (outgoingSequenceFlow != null) {
            execution.setCurrentFlowElement(outgoingSequenceFlow);
        } else {
            if (defaultSequenceFlow != null) {
                execution.setCurrentFlowElement(defaultSequenceFlow);
            } else {

                // 找不到序列流，甚至找不到默认序列流
                throw new FlowableException("No outgoing sequence flow of the exclusive gateway '" + exclusiveGateway.getId() + "' could be selected for continuing the process");
            }
        }

        super.leave(execution);
    }
}
