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
package org.activiti.engine.impl.bpmn.parser.handler;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.pvm.process.ScopeImpl;
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.EventDefinition;
import org.flowable.bpmn.model.IntermediateCatchEvent;
import org.flowable.bpmn.model.MessageEventDefinition;
import org.flowable.bpmn.model.SignalEventDefinition;
import org.flowable.bpmn.model.TimerEventDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 中间捕获事件解析处理器
 *
 * @author Joram Barrez
 */
public class IntermediateCatchEventParseHandler extends AbstractFlowNodeBpmnParseHandler<IntermediateCatchEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(IntermediateCatchEventParseHandler.class);

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return IntermediateCatchEvent.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, IntermediateCatchEvent event) {

        ActivityImpl nestedActivity = null;
        EventDefinition eventDefinition = null;
        if (!event.getEventDefinitions().isEmpty()) {
            eventDefinition = event.getEventDefinitions().get(0);
        }

        if (eventDefinition == null) {

            nestedActivity = createActivityOnCurrentScope(bpmnParse, event, BpmnXMLConstants.ELEMENT_EVENT_CATCH);
            nestedActivity.setAsync(event.isAsynchronous());
            nestedActivity.setExclusive(!event.isNotExclusive());

        } else {

            ScopeImpl scope = bpmnParse.getCurrentScope();
            String eventBasedGatewayId = getPrecedingEventBasedGateway(bpmnParse, event);
            if (eventBasedGatewayId != null) {
                ActivityImpl gatewayActivity = scope.findActivity(eventBasedGatewayId);
                nestedActivity = createActivityOnScope(bpmnParse, event, BpmnXMLConstants.ELEMENT_EVENT_CATCH, gatewayActivity);
            } else {
                nestedActivity = createActivityOnScope(bpmnParse, event, BpmnXMLConstants.ELEMENT_EVENT_CATCH, scope);
            }

            nestedActivity.setAsync(event.isAsynchronous());
            nestedActivity.setExclusive(!event.isNotExclusive());

            // 捕获事件行为对于所有类型都是相同的
            nestedActivity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createIntermediateCatchEventActivityBehavior(event));

            if (eventDefinition instanceof TimerEventDefinition
                    || eventDefinition instanceof SignalEventDefinition
                    || eventDefinition instanceof MessageEventDefinition) {

                bpmnParse.getBpmnParserHandlers().parseElement(bpmnParse, eventDefinition);

            } else {
                // 不支持事件{}的中间捕获事件类型
                LOGGER.warn("Unsupported intermediate catch event type for event {}", event.getId());
            }
        }
    }

}
