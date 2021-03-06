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
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????.
     */
    @Test
    public void testAddAndRemoveEventListenerAllEvents() throws Exception {
        // ???????????????????????????????????????????????????
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // ???????????????????????????????????????
        dispatcher.addEventListener(newListener);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);

        // ????????????
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).hasSize(2);
        assertThat(newListener.getEventsReceived().get(0)).isEqualTo(event1);
        assertThat(newListener.getEventsReceived().get(1)).isEqualTo(event2);

        // ????????????????????????????????????????????????????????????
        dispatcher.removeEventListener(newListener);
        newListener.clearEventsReceived();
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).isEmpty();
    }

    /**
     * ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????.
     */
    @Test
    public void testAddAndRemoveEventListenerTyped() throws Exception {
        // ???????????????????????????????????????????????????
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // ???????????????????????????????????????
        dispatcher.addEventListener(newListener, FlowableEngineEventType.ENTITY_CREATED, FlowableEngineEventType.ENTITY_DELETED);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_DELETED);
        FlowableEntityEventImpl event3 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_UPDATED);

        // ???????????????????????????????????????????????????????????????
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event3, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).hasSize(2);
        assertThat(newListener.getEventsReceived().get(0)).isEqualTo(event1);
        assertThat(newListener.getEventsReceived().get(1)).isEqualTo(event2);

        // ????????????????????????????????????????????????????????????
        dispatcher.removeEventListener(newListener);
        newListener.clearEventsReceived();
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).isEmpty();
    }

    /**
     * ??????????????????????????????????????????????????????
     */
    @Test
    public void testAddAndRemoveEventListenerTypedNullType() throws Exception {

        // ???????????????????????????????????????????????????
        TestFlowableEventListener newListener = new TestFlowableEventListener();

        // ???????????????????????????????????????
        dispatcher.addEventListener(newListener, (FlowableEngineEventType) null);

        TaskServiceConfiguration taskServiceConfiguration = (TaskServiceConfiguration) processEngineConfiguration.getServiceConfigurations()
                .get(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG);
        FlowableEntityEventImpl event1 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_CREATED);
        FlowableEntityEventImpl event2 = new FlowableEntityEventImpl(taskServiceConfiguration.getTaskEntityManager().create(),
                FlowableEngineEventType.ENTITY_DELETED);

        // ?????????????????????????????????????????????????????????
        dispatcher.dispatchEvent(event1, processEngineConfiguration.getEngineCfgKey());
        dispatcher.dispatchEvent(event2, processEngineConfiguration.getEngineCfgKey());

        assertThat(newListener.getEventsReceived()).isEmpty();
    }

    /**
     * ??????Flowable????????????????????????????????????{@link BaseEntityEventListener}???
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

        // ??????????????????
        dispatcher.dispatchEvent(createEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isCreateReceived()).isTrue();
        assertThat(listener.isUpdateReceived()).isFalse();
        assertThat(listener.isCustomReceived()).isFalse();
        assertThat(listener.isInitializeReceived()).isFalse();
        assertThat(listener.isDeleteReceived()).isFalse();
        listener.reset();

        // ??????????????????
        dispatcher.dispatchEvent(updateEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isUpdateReceived()).isTrue();
        assertThat(listener.isCreateReceived()).isFalse();
        assertThat(listener.isCustomReceived()).isFalse();
        assertThat(listener.isDeleteReceived()).isFalse();
        listener.reset();

        // ??????????????????
        dispatcher.dispatchEvent(deleteEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isDeleteReceived()).isTrue();
        assertThat(listener.isCreateReceived()).isFalse();
        assertThat(listener.isCustomReceived()).isFalse();
        assertThat(listener.isUpdateReceived()).isFalse();
        listener.reset();

        // ??????????????????
        dispatcher.dispatchEvent(otherEvent, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isCustomReceived()).isTrue();
        assertThat(listener.isCreateReceived()).isFalse();
        assertThat(listener.isUpdateReceived()).isFalse();
        assertThat(listener.isDeleteReceived()).isFalse();
        listener.reset();

        // ???????????????????????????
        listener = new TestBaseEntityEventListener(org.flowable.task.api.Task.class);

        // ??????????????????????????????
        dispatcher.addEventListener(listener);
        dispatcher.dispatchEvent(createEvent, processEngineConfiguration.getEngineCfgKey());

        assertThat(listener.isCreateReceived()).isTrue();
        listener.reset();

        // ?????????????????????????????????
        FlowableEntityEventImpl createEventForExecution = new FlowableEntityEventImpl(new ExecutionEntityImpl(), FlowableEngineEventType.ENTITY_CREATED);

        dispatcher.dispatchEvent(createEventForExecution, processEngineConfiguration.getEngineCfgKey());
        assertThat(listener.isCreateReceived()).isFalse();
    }

    /**
     * ????????????????????????????????????????????????
     */
    @Test
    public void testExceptionInListener() throws Exception {
        // ???????????????????????????????????????
        TestExceptionFlowableEventListener listener = new TestExceptionFlowableEventListener(false);
        TestFlowableEventListener secondListener = new TestFlowableEventListener();

        dispatcher.addEventListener(listener);
        dispatcher.addEventListener(secondListener);

        FlowableEngineEventImpl event = new FlowableProcessEventImpl(FlowableEngineEventType.ENTITY_CREATED);
        assertThatCode(() -> {
            dispatcher.dispatchEvent(event, processEngineConfiguration.getEngineCfgKey());
        }).doesNotThrowAnyException();
        assertThat(secondListener.getEventsReceived()).hasSize(1);

        // ???????????????
        dispatcher.removeEventListener(listener);
        dispatcher.removeEventListener(secondListener);

        // ????????????????????????????????????
        listener = new TestExceptionFlowableEventListener(true);
        secondListener = new TestFlowableEventListener();
        dispatcher.addEventListener(listener);
        dispatcher.addEventListener(secondListener);

        assertThatThrownBy(() -> dispatcher.dispatchEvent(event, processEngineConfiguration.getEngineCfgKey()))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Test exception");

        // ?????????????????????????????????
        assertThat(secondListener.getEventsReceived()).isEmpty();
    }

    /**
     * ??????Flowable??????????????????{ @link FlowableEngineeEventType }???????????????????????????????????????????????????????????????????????????
     * {@link ProcessEngineConfigurationImpl#setTypedEventListeners???java.util.Map???}???
     */
    @Test
    public void testActivitiEventTypeParsing() throws Exception {
        // ?????????null????????????
        FlowableEngineEventType[] types = FlowableEngineEventType.getTypesFromString(null);
        assertThat(types).isEmpty();

        // ?????????????????????
        types = FlowableEngineEventType.getTypesFromString("");
        assertThat(types).isEmpty();

        // ?????????
        types = FlowableEngineEventType.getTypesFromString("ENTITY_CREATED");
        assertThat(types).hasSize(1);
        assertThat(types[0]).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);

        // ?????????
        types = FlowableEngineEventType.getTypesFromString("ENTITY_CREATED,ENTITY_DELETED");
        assertThat(types).hasSize(2);
        assertThat(types[0]).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(types[1]).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);

        // ??????????????????
        types = FlowableEngineEventType.getTypesFromString(",ENTITY_CREATED,,ENTITY_DELETED,,,");
        assertThat(types).hasSize(2);
        assertThat(types[0]).isEqualTo(FlowableEngineEventType.ENTITY_CREATED);
        assertThat(types[1]).isEqualTo(FlowableEngineEventType.ENTITY_DELETED);

        // ??????????????????
        assertThatThrownBy(() -> FlowableEngineEventType.getTypesFromString("WHOOPS,ENTITY_DELETED"))
                .isExactlyInstanceOf(FlowableIllegalArgumentException.class)
                .hasMessage("Invalid event-type: WHOOPS");
    }
}
