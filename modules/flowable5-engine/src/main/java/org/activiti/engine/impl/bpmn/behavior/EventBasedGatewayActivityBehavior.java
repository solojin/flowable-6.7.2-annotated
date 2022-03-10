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

import org.flowable.engine.delegate.DelegateExecution;

/**
 * 基于事件的网关活动行为类
 *
 * @author Daniel Meyer
 */
public class EventBasedGatewayActivityBehavior extends FlowNodeActivityBehavior {

    @Override
    public void execute(DelegateExecution execution) {
        // 基于事件的网关实际上什么都不做
        // 忽略流出序列流（它们只针对图表进行分析）
    }

}
