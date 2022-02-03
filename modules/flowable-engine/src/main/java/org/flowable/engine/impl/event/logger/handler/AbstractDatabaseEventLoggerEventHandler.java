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
package org.flowable.engine.impl.event.logger.handler;

import java.util.Date;
import java.util.Map;

import org.flowable.common.engine.api.delegate.event.FlowableEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Joram Barrez
 */
public abstract class AbstractDatabaseEventLoggerEventHandler implements EventLoggerEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDatabaseEventLoggerEventHandler.class);

    protected FlowableEvent event;
    protected Date timeStamp;
    protected ObjectMapper objectMapper;

    // 空参构造方法
    public AbstractDatabaseEventLoggerEventHandler() {
    }

    protected EventLogEntryEntity createEventLogEntry(Map<String, Object> data) {
        return createEventLogEntry(null, null, null, null, data);
    }

    protected EventLogEntryEntity createEventLogEntry(String processDefinitionId, String processInstanceId, String executionId, String taskId, Map<String, Object> data) {
        return createEventLogEntry(event.getType().name(), processDefinitionId, processInstanceId, executionId, taskId, data);
    }

    protected EventLogEntryEntity createEventLogEntry(String type, String processDefinitionId, String processInstanceId, String executionId, String taskId, Map<String, Object> data) {

        // 从命令上下文工具类中获取事件日志对象管理器
        EventLogEntryEntity eventLogEntry = CommandContextUtil.getEventLogEntryEntityManager().create();
        // 设置流程定义ID
        eventLogEntry.setProcessDefinitionId(processDefinitionId);
        // 设置流程实例ID
        eventLogEntry.setProcessInstanceId(processInstanceId);
        // 设置执行器ID
        eventLogEntry.setExecutionId(executionId);
        // 设置任务ID
        eventLogEntry.setTaskId(taskId);
        // 设置类型
        eventLogEntry.setType(type);
        eventLogEntry.setTimeStamp(timeStamp);
        putInMapIfNotNull(data, Fields.TIMESTAMP, timeStamp);

        // 当前用户
        String userId = Authentication.getAuthenticatedUserId();
        if (userId != null) {
            // 事件日志对象中写入当前用户ID
            eventLogEntry.setUserId(userId);
            putInMapIfNotNull(data, "userId", userId);
        }

        // 当前租户
        if (!data.containsKey(Fields.TENANT_ID) && processDefinitionId != null) {
            // 根据流程定义ID获取流程定义
            ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);
            if (processDefinition != null && !ProcessEngineConfigurationImpl.NO_TENANT_ID.equals(processDefinition.getTenantId())) {
                putInMapIfNotNull(data, Fields.TENANT_ID, processDefinition.getTenantId());
            }
        }

        try {
            eventLogEntry.setData(objectMapper.writeValueAsBytes(data));
        } catch (Exception e) {
            // 无法序列化事件数据。数据不会写入数据库
            LOGGER.warn("Could not serialize event data. Data will not be written to the database", e);
        }

        return eventLogEntry;

    }

    // 重写方法，设置事件
    @Override
    public void setEvent(FlowableEvent event) {
        this.event = event;
    }

    // 重写方法，设置时间戳
    @Override
    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    // 重写方法，设置对象映射
    @Override
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // 辅助器方法 //////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    // 从事件获取对象
    public <T> T getEntityFromEvent() {
        return (T) ((FlowableEntityEvent) event).getEntity();
    }

    // value非空时，向map中写入新值
    public void putInMapIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }

}
