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
package org.flowable.cdi;

import java.util.Date;

import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * Signifies an event that is happening / has happened during the execution of a business process.
 * 
 * @author Daniel Meyer
 */
public interface BusinessProcessEvent {

    /**
     * @return the process definition in which the event is happening / has happened
     */
    ProcessDefinition getProcessDefinition();

    /**
     * @return the id of the activity the process is currently in / was in at the moment the event was fired.
     */
    String getActivityId();

    /**
     * @return the name of the transition being taken / that was taken. (null, if this event is not of type {@link BusinessProcessEventType#TAKE}
     */
    String getTransitionName();

    /**
     * @return the id of the {@link ProcessInstance} this event corresponds to
     */
    String getProcessInstanceId();

    /**
     * @return the id of the {@link Execution} this event corresponds to
     */
    String getExecutionId();

    /**
     * @return the type of the event
     */
    BusinessProcessEventType getType();

    /**
     * @return the timestamp indicating the local time at which the event was fired.
     */
    Date getTimeStamp();

    /**
     * @return the variable scope associated with the event
     */
    VariableScope getVariableScope();

}
