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
package org.flowable.engine.impl.bpmn.parser.handler;

import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.SubProcess;
import org.flowable.engine.impl.bpmn.parser.BpmnParse;

/**
 * 子流程解析处理器
 *
 * @author Joram Barrez
 */
public class SubProcessParseHandler extends AbstractActivityBpmnParseHandler<SubProcess> {

    @Override
    protected Class<? extends BaseElement> getHandledType() {
        return SubProcess.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, SubProcess subProcess) {

        // 策略模式，动态设置子流程解析行为
        subProcess.setBehavior(bpmnParse.getActivityBehaviorFactory().createSubprocessActivityBehavior(subProcess));

        bpmnParse.processFlowElements(subProcess.getFlowElements());
        processArtifacts(bpmnParse, subProcess.getArtifacts());

        // 没有用于事件子流程的数据对象
        /*
         * if (!(subProcess instanceof EventSubProcess)) { // parse out any data objects from the template in order to set up the necessary process variables Map<String, Object> variables =
         * processDataObjects(bpmnParse, subProcess.getDataObjects(), activity); activity.setVariables(variables); }
         * 
         * bpmnParse.removeCurrentScope(); bpmnParse.removeCurrentSubProcess();
         * 
         * if (subProcess.getIoSpecification() != null) { IOSpecification ioSpecification = createIOSpecification(bpmnParse, subProcess.getIoSpecification());
         * activity.setIoSpecification(ioSpecification); }
         */

    }

}
