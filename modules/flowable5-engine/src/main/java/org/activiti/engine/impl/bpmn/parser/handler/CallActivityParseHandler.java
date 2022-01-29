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
import org.flowable.bpmn.constants.BpmnXMLConstants;
import org.flowable.bpmn.model.BaseElement;
import org.flowable.bpmn.model.CallActivity;

/**
 * @author Joram Barrez
 */
public class CallActivityParseHandler extends AbstractActivityBpmnParseHandler<CallActivity> {

    @Override
    public Class<? extends BaseElement> getHandledType() {
        return CallActivity.class;
    }

    @Override
    protected void executeParse(BpmnParse bpmnParse, CallActivity callActivity) {

        ActivityImpl activity = createActivityOnCurrentScope(bpmnParse, callActivity, BpmnXMLConstants.ELEMENT_CALL_ACTIVITY);
        activity.setScope(true);
        activity.setAsync(callActivity.isAsynchronous());
        activity.setExclusive(!callActivity.isNotExclusive());
        activity.setActivityBehavior(bpmnParse.getActivityBehaviorFactory().createCallActivityBehavior(callActivity));
    }

}