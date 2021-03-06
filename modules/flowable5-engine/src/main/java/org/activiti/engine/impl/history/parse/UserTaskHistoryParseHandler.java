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
package org.activiti.engine.impl.history.parse;

import org.activiti.engine.impl.bpmn.parser.BpmnParse;
import org.activiti.engine.impl.bpmn.parser.handler.AbstractBpmnParseHandler;
import org.activiti.engine.impl.bpmn.parser.handler.UserTaskParseHandler;
import org.activiti.engine.impl.history.handler.UserTaskAssignmentHandler;
import org.activiti.engine.impl.history.handler.UserTaskIdHandler;
import org.activiti.engine.impl.task.TaskDefinition;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.delegate.TaskListener;

/**
 * 用户任务历史解析器
 * 负责解析用户任务实例对象并为其添加类型为代理assigment和创建create的监听器
 *
 * @author Joram Barrez
 */
public class UserTaskHistoryParseHandler extends AbstractBpmnParseHandler<UserTask> {

    protected static final UserTaskAssignmentHandler USER_TASK_ASSIGNMENT_HANDLER = new UserTaskAssignmentHandler();

    protected static final UserTaskIdHandler USER_TASK_ID_HANDLER = new UserTaskIdHandler();

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return UserTask.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, UserTask element) {
        TaskDefinition taskDefinition = (TaskDefinition) bpmnParse.getCurrentActivity().getProperty(UserTaskParseHandler.PROPERTY_TASK_DEFINITION);
        // 添加ASSIGNMENT类型的监听器
        taskDefinition.addTaskListener(TaskListener.EVENTNAME_ASSIGNMENT, USER_TASK_ASSIGNMENT_HANDLER);
        // 添加 CREATE类型的监听器
        taskDefinition.addTaskListener(TaskListener.EVENTNAME_CREATE, USER_TASK_ID_HANDLER);
    }

}
