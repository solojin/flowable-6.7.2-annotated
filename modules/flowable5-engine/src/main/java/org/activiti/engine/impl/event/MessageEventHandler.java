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

package org.activiti.engine.impl.event;

import org.activiti.engine.delegate.event.impl.ActivitiEventBuilder;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;

/**
 * @author Daniel Meyer
 */
public class MessageEventHandler extends AbstractEventHandler {

    public static final String EVENT_HANDLER_TYPE = "message";

    @Override
    public String getEventHandlerType() {
        return EVENT_HANDLER_TYPE;
    }

    @Override
    public void handleEvent(EventSubscriptionEntity eventSubscription, Object payload, CommandContext commandContext) {
        // As stated in the ActivitiEventType java-doc, the message-event is thrown before the actual message has been sent
        if (commandContext.getProcessEngineConfiguration().getEventDispatcher().isEnabled()) {
            commandContext.getProcessEngineConfiguration().getEventDispatcher().dispatchEvent(
                    ActivitiEventBuilder.createMessageEvent(FlowableEngineEventType.ACTIVITY_MESSAGE_RECEIVED, eventSubscription.getActivityId(), eventSubscription.getEventName(),
                            payload, eventSubscription.getExecutionId(), eventSubscription.getProcessInstanceId(), eventSubscription.getExecution().getProcessDefinitionId()),
                    EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG);
        }

        super.handleEvent(eventSubscription, payload, commandContext);
    }

}
