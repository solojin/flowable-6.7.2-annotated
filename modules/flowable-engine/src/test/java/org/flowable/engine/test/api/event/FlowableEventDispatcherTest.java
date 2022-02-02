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
package org.flowable.engine.test.api.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.impl.event.FlowableEngineEventImpl;
import org.flowable.common.engine.impl.event.FlowableEventDispatcherImpl;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.delegate.event.BaseEntityEventListener;
import org.flowable.engine.delegate.event.impl.FlowableEntityEventImpl;
import org.flowable.engine.delegate.event.impl.FlowableProcessEventImpl;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.test.PluggableFlowableTestCase;
import org.flowable.task.service.TaskServiceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Frederik Heremans
 */
public class FlowableEventDispatcherTest extends PluggableFlowableTestCase {

    protected FlowableEventDispatcher dispatcher;

    @BeforeEach
    protected void setUp() throws Exception {

        dispatcher = new FlowableEventDispatcherImpl();
    }

    /**
     * 测试添加监听器并检查是否向其发送了事件，还检查删除后是否未收到任何事件.
     */
    @Test
    public void testAddAndRemoveEventListenerAllEvents() throws Exception {
        // 创建一个只将事件添加到列表的监听器
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // 将事件监听器添加到调度程序
        dispatcher.addEventListener(newListener);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);

        // 调度事件
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).hasSize(2);
        assertThat(newListener.getEventsReceived().get(0)).isEqualTo(event1);
        assertThat(newListener.getEventsReceived().get(1)).isEqualTo(event2);

        // 删除监听器并再次分派事件，不应调用监听器
        dispatcher.removeEventListener(newListener);
        newListener.clearEventsReceived();
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).isEmpty();
    }

    /**
     * 测试添加一个监听器，并检查是否向它发送了事件，以及它注册的类型。还检查删除后是否未收到任何事件.
     */
    @Test
    public void testAddAndRemoveEventListenerTyped() throws Exception {
        // 创建一个只将事件添加到列表的监听器
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // 将事件监听器添加到调度程序
        dispatcher.addEventListener(newListener, FlowableEngineEventType.ENTITY_CREATED, FlowableEngineEventType.ENTITY_DELETED);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_DELETED);
        FlowableEntityEventImpl event3 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_UPDATED);

        // 分派事件，三分之二的事件应该已经进入监听器
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event3, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).hasSize(2);
        assertThat(newListener.getEventsReceived().get(0)).isEqualTo(event1);
        assertThat(newListener.getEventsReceived().get(1)).isEqualTo(event2);

        // 删除监听器并再次分派事件，不应调用监听器
        dispatcher.removeEventListener(newListener);
        newListener.clearEventsReceived();
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).isEmpty();
    }

    /**
     * 测试是否从未调用添加空类型的监听器。
     */
    @Test
    public void testAddAndRemoveEventListenerTypedNullType() throws Exception {

        // 创建一个只将事件添加到列表的监听器
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // 将事件监听器添加到调度程序
        dispatcher.addEventListener(newListener, (FlowableEngineEventType) null);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_DELETED);

        // 分派事件时，所有事件都应该已进入监听器
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).isEmpty();
    }

    /**
     * 测试Flowable附带的基础实体事件监听器{@link BaseEntityEventListener}。
     */
    @Test
    public void testBaseEntityEventListener() throws Exception {
        TestBaseEntityEventListener listener = new TestBaseEntityEventListener();

        dispatcher.addEventListener(listener);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl createEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl deleteEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_DELETED);
        FlowableEntityEventImpl updateEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_UPDATED);
        FlowableEntityEventImpl otherEvent = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.CUSTOM);

        // 调度创建事件
        dispatcher.dispatchEvent(createEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isCreateReceived()).isTrue();
        assertThat(listener.isUpdateReceived()).isFalse();
        assertThat(listener.isCustomReceived()).isFalse();
        assertThat(listener.isInitializeReceived()).isFalse();
        assertThat(listener.isDeleteReceived()).isFalse();
        listener.reset();

        // 调度更新事件
        dispatcher.dispatchEvent(updateEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isUpdateReceived()).isTrue();
        assertThat(listener.isCreateReceived()).isFalse();
        assertThat(listener.isCustomReceived()).isFalse();
        assertThat(listener.isDeleteReceived()).isFalse();
        listener.reset();

        // 调度删除事件
        dispatcher.dispatchEvent(deleteEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isDeleteReceived()).isTrue();
        assertThat(listener.isCreateReceived()).isFalse();
        assertThat(listener.isCustomReceived()).isFalse();
        assertThat(listener.isUpdateReceived()).isFalse();
        listener.reset();

        // 调度其他事件
        dispatcher.dispatchEvent(otherEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isCustomReceived()).isTrue();
        assertThat(listener.isCreateReceived()).isFalse();
        assertThat(listener.isUpdateReceived()).isFalse();
        assertThat(listener.isDeleteReceived()).isFalse();
        listener.reset();

        // 测试类型实体监听器
        listener = new TestBaseEntityEventListener(org.flowable.task.api.Task.class);

        // 应收到任务的调度事件
        dispatcher.addEventListener(listener);
        dispatcher.dispatchEvent(createEvent, processEngineConfiguration.getEngineCfgKey());

        assertThat(listener.isCreateReceived()).isTrue();
        listener.reset();

        // 不应接收执行的调度事件
        FlowableEntityEventImpl createEventForExecution = new FlowableEntityEventImpl(new ExecutionEntityImpl(), FlowableEngineEventType.ENTITY_CREATED);

        dispatcher.dispatchEvent(createEventForExecution, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isCreateReceived()).isFalse();
    }

    /**
     * 在监听器中发生异常时测试调度行为
     */
    @Test
    public void testExceptionInListener() throws Exception {
        // 创建不强制分派失败的侦听器
        TestExceptionFlowableEventListener listener = new TestExceptionFlowableEventListener(false);
        TestFlowableEventListener secondListener = new TestFlowableEventListener();

        dispatcher.addEventListener(listener);
        dispatcher.addEventListener(secondListener);

        FlowableEngineEventImpl event = new FlowableProcessEventImpl(FlowableEngineEventType.ENTITY_CREATED);
        assertThatCode(() -> {
            dispatcher.dispatchEvent(event, processEngineConfiguration.getEngineCfgKey());
        }).doesNotThrowAnyException();
        assertThat(secondListener.getEventsReceived()).hasSize(1);

        // 删除监听器
        dispatcher.removeEventListener(listener);
        dispatcher.removeEventListener(secondListener);

        // 创建强制分派失败的监听器
        listener = new TestExceptionFlowableEventListener(true);
        secondListener = new TestFlowableEventListener();
        dispatcher.addEventListener(listener);
        dispatcher.addEventListener(secondListener);

        assertThatThrownBy(() -> dispatcher.dispatchEvent(event, processEngineConfiguration.getEngineCfgKey()))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        // 不应该调用第二个侦听器
        assertThat(secondListener.getEventsReceived()).isEmpty();
    }

    /**
     * 测试Flowable引擎事件类型{ @link FlowableEngineeEventType }列表中字符串值（和列表）的转换，用于流程引擎的配置
     * {@link ProcessEngineConfigurationImpl#setTypedEventListeners（java.util.Map）}。
     */
    @Test
    public void testActivitiEventTypeParsing() throws Exception {
        // 使用空null进行检查
        FlowableEngineEventType[] types = FlowableEngineEventType.getTypesFromString(null);
        assertThat(types).isEmpty();

        // 用空字符串检查
        types = FlowableEngineEventType.getTypesFromString("");
        assertThat(types).isEmpty();

        // 单个值
        types = FlowableEngineEventType.getTypesFromString("ENTITY_CREATED");
        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);

        // 多个值
        types = FlowableEngineEventType.getTypesFromString("ENTITY_CREATED,ENTITY_DELETED");
        assertThat(types).hasSize(2);
        assertThat(types[0]).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(types[1]).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);

        // 应忽略分隔符
        types = FlowableEngineEventType.getTypesFromString(",ENTITY_CREATED,,ENTITY_DELETED,,,");
        assertThat(types).hasSize(2);
        assertThat(types[0]).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(types[1]).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);

        // 无效的类型名
        assertThatThrownBy(() -> FlowableEngineEventType.getTypesFromString("WHOOPS,ENTITY_DELETED"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Invalid event-type: WHOOPS");
    }
}
