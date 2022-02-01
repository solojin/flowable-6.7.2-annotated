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
package org.flowable.common.engine.api.delegate.event;

import java.util.Collection;
import java.util.Collections;

/**
 * 描述一个类，该类监听引擎调度的事件{@link FlowableEvent}.
 * 
 * @author Frederik Heremans
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public interface FlowableEventListener {

    /**
     * 在触发事件时调用
     * 
     * @param event 事件
     */
    void onEvent(FlowableEvent event);

    /**
     * @return 当此监听器执行引发异常时，当前操作是否应失败。
     */
    boolean isFailOnException();
    
    /**
     * @return 在事务生命周期事件上（提交或回滚之前/之后），返回事件发生时是否立即触发此事件监听器
     */
    boolean isFireOnTransactionLifecycleEvent();
    
    /**
     * @return 如果非空，则指示当前事务生命周期中应该触发事件的时间点。
     */
    String getOnTransaction();
    
    /**
     * 此事件监听器需要注册的事件类型
     *
     * @return 此监听器应为其自身注册的特定事件类型
     */
    default Collection<? extends FlowableEventType> getTypes() {
        return Collections.emptySet();
    }

}
