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
import org.activiti.engine.impl.history.handler.ProcessInstanceEndHandler;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.Process;

/**
 * 流程历史解析器
 * 负责解析Process实例对象并为其添加事件类型为end的监听器
 *
 * @author Joram Barrez
 */
public class ProcessHistoryParseHandler extends AbstractBpmnParseHandler<Process> {

    protected static final ProcessInstanceEndHandler PROCESS_INSTANCE_END_HANDLER = new ProcessInstanceEndHandler();

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return Process.class;
    }

    // 执行解析，为其添加类型为end的监听器
    @Override
    protected void executeParse(BpmnParse bpmnParse, Process element) {
        bpmnParse.getCurrentProcessDefinition().addExecutionListener(org.activiti.engine.impl.pvm.PvmEvent.EVENTNAME_END, PROCESS_INSTANCE_END_HANDLER);
    }

}
