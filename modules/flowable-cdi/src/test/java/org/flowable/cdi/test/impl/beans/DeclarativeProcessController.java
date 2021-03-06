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
package org.flowable.cdi.test.impl.beans;

import org.flowable.cdi.annotation.CompleteTask;
import org.flowable.cdi.annotation.ProcessVariable;
import org.flowable.cdi.annotation.StartProcess;

/**
 * 
 * @author Daniel Meyer
 */
public class DeclarativeProcessController {

    @ProcessVariable
    String name; // this is going to be set as a process variable

    @StartProcess(name = "Name of the Process")
    public void startProcessByName() {
        name = "Flowable";
    }

    @StartProcess("keyOfTheProcess")
    public void startProcessByKey() {
        name = "Flowable";
    }

    @CompleteTask(endConversation = false)
    public void completeTask() {
    }

    @CompleteTask(endConversation = true)
    public void completeTaskEndConversation() {
    }

}
