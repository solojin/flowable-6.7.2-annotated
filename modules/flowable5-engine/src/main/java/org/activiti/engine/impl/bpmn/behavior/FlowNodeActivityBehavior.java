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
package org.activiti.engine.impl.bpmn.behavior;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.impl.pvm.delegate.ActivityExecution;
import org.activiti.engine.impl.pvm.delegate.SignallableActivityBehavior;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * 流节点活动行为类
 *
 * 所有“可连接”BPMN 2.0流程元素的超类：任务、网关和事件。这意味着任何子类都可以是sequenceflow的源或目标。
 * 与BPMN 2.0中的“流节点”（flownode）概念相对应。
 * 
 * @author Joram Barrez
 */
public abstract class FlowNodeActivityBehavior implements SignallableActivityBehavior {

    protected BpmnActivityBehavior bpmnActivityBehavior = new BpmnActivityBehavior();

    /**
     * 默认行为：无额外功能的离开活动。
     */
    @Override
    public void execute(DelegateExecution execution) {
        leave((ActivityExecution) execution);
    }

    /**
     * 离开BPMN 2.0活动的默认方式是：评估流出序列流上的条件，并将评估结果置为真。
     */
    protected void leave(ActivityExecution execution) {
        bpmnActivityBehavior.performDefaultOutgoingBehavior(execution);
    }

    /**
     * 忽略条件的离开
     */
    protected void leaveIgnoreConditions(ActivityExecution activityContext) {
        bpmnActivityBehavior.performIgnoreConditionsOutgoingBehavior(activityContext);
    }

    @Override
    public void signal(ActivityExecution execution, String signalName, Object signalData) throws Exception {
        // 接受信号的具体活动行为应重写该方法
        throw new ActivitiException("this activity doesn't accept signals");
    }

}
