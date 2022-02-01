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

/**
 * 调度器（Dispatcher）允许向Flowable引擎添加和删除事件监听器{@link FlowableEventListener}，并将事件{@link FlowableEvent}分配给所有注册的监听器。
 * 
 * @author Frederik Heremans
 */
public interface FlowableEventDispatcher {

    /**
     * 添加一个事件监听器，调度器将通知该监听器所有事件.
     * 
     * @param listenerToAdd
     *            要添加的监听器
     */
    void addEventListener(FlowableEventListener listenerToAdd);

    /**
     * 添加一个事件监听器，该监听器仅在给定类型的事件发生时收到通知.
     * 
     * @param listenerToAdd
     *            要添加的监听器
     * @param types
     *            应通知监听器的事件类型
     */
    void addEventListener(FlowableEventListener listenerToAdd, FlowableEventType... types);

    /**
     * 从此调度器中删除给定的监听器。无论最初注册的是哪种类型，监听器都不会再收到通知.
     * 
     * @param listenerToRemove
     *            要删除的监听器
     */
    void removeEventListener(FlowableEventListener listenerToRemove);

    /**
     * 将给定事件分派给任何已注册的监听器.
     * 
     * @param event
     *            要调度的事件.
     * @param engineType
     *            要调度的引擎类型
     */
    void dispatchEvent(FlowableEvent event, String engineType);

    /**
     * @param enabled
     *            设置事件调度，为true则启用.
     */
    void setEnabled(boolean enabled);

    /**
     * @return 如果启用了事件调度器，则为true.
     */
    boolean isEnabled();

}
