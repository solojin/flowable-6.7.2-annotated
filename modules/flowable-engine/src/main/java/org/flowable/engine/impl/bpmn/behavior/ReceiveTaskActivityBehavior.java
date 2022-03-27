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

package org.flowable.engine.impl.bpmn.behavior;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;

/**
 * 接收任务活动行为
 *
 * 接收任务是等待接收消息的等待状态。
 * 
 * 目前，唯一支持的消息是外部触发器，通过调用{@link RuntimeService#trigger（String）}操作给出。
 *
 * @author Joram Barrez
 */
public class ReceiveTaskActivityBehavior extends TaskActivityBehavior {

    private static final long serialVersionUID = 1L;

    @Override
    public void execute(DelegateExecution execution) {
        // 不做事：等待状态行为
    }

    @Override
    public void trigger(DelegateExecution execution, String signalName, Object data) {
        leave(execution);
    }

}
