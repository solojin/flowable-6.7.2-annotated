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
package org.flowable.engine.delegate.event;

import org.flowable.common.engine.api.delegate.event.AbstractFlowableEventListener;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;

/**
 * 在实现｛@link FlowableEventListener｝时可以使用的基本事件侦听器，以便在创建、更新、删除实体或发生其他与实体相关的事件时获得通知.
 *
 * 重写<code>onXX（..）</code>方法以相应地响应实体更改。
 * 
 * @author Frederik Heremans
 * 
 */
public class BaseEntityEventListener extends AbstractFlowableEventListener {

    protected boolean failOnException;
    protected Class<?> entityClass;

    /**
     * Create a new BaseEntityEventListener, notified when an event that targets any type of entity is received. Returning true when {@link #isFailOnException()} is called.
     */
    public BaseEntityEventListener() {
        this(true, null);
    }

    /**
     * Create a new BaseEntityEventListener.
     * 
     * @param failOnException
     *            return value for {@link #isFailOnException()}.
     */
    public BaseEntityEventListener(boolean failOnException) {
        this(failOnException, null);
    }

    public BaseEntityEventListener(boolean failOnException, Class<?> entityClass) {
        this.failOnException = failOnException;
        this.entityClass = entityClass;
    }

    @Override
    public final void onEvent(FlowableEvent event) {
        if (isValidEvent(event)) {
            // 检查此事件
            if (event.getType() == FlowableEngineEventType.ENTITY_CREATED) {
                onCreate(event);
            } else if (event.getType() == FlowableEngineEventType.ENTITY_INITIALIZED) {
                onInitialized(event);
            } else if (event.getType() == FlowableEngineEventType.ENTITY_DELETED) {
                onDelete(event);
            } else if (event.getType() == FlowableEngineEventType.ENTITY_UPDATED) {
                onUpdate(event);
            } else {
                // 实体特定事件
                onEntityEvent(event);
            }
        }
    }

    @Override
    public boolean isFailOnException() {
        return failOnException;
    }

    /**
     * @return true, if the event is an {@link FlowableEntityEvent} and (if needed) the entityClass set in this instance, is assignable from the entity class in the event.
     */
    protected boolean isValidEvent(FlowableEvent event) {
        boolean valid = false;
        if (event instanceof FlowableEntityEvent) {
            if (entityClass == null) {
                valid = true;
            } else {
                valid = entityClass.isAssignableFrom(((FlowableEntityEvent) event).getEntity().getClass());
            }
        }
        return valid;
    }

    /**
     * Called when an entity create event is received.
     */
    protected void onCreate(FlowableEvent event) {
        // Default implementation is a NO-OP
    }

    /**
     * Called when an entity initialized event is received.
     */
    protected void onInitialized(FlowableEvent event) {
        // Default implementation is a NO-OP
    }

    /**
     * Called when an entity delete event is received.
     */
    protected void onDelete(FlowableEvent event) {
        // Default implementation is a NO-OP
    }

    /**
     * Called when an entity update event is received.
     */
    protected void onUpdate(FlowableEvent event) {
        // Default implementation is a NO-OP
    }

    /**
     * Called when an event is received, which is not a create, an update or delete.
     */
    protected void onEntityEvent(FlowableEvent event) {
        // Default implementation is a NO-OP
    }
}
