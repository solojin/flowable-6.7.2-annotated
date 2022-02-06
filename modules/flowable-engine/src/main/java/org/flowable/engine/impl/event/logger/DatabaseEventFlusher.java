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
package org.flowable.engine.impl.event.logger;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.event.logger.handler.EventLoggerEventHandler;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Joram Barrez
 */
public class DatabaseEventFlusher extends AbstractEventFlusher {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseEventFlusher.class);

    @Override
    public void closing(CommandContext commandContext) {

        if (commandContext.getException() != null) {
            return; //未关注事件异常
        }

        // 根据命令上下文工具类获取流程引擎配置类
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        // 根据流程引擎配置类获取EventLogEntryEntityManager类对象，它负责 ACT_EVT_LOG 表的操作
        EventLogEntryEntityManager eventLogEntryEntityManager = processEngineConfiguration.getEventLogEntryEntityManager();
        // 遍历事件日志处理器对象
        for (EventLoggerEventHandler eventHandler : eventHandlers) {
            try {
                // 通过事件日志实体对象管理器的插入方法，新增事件处理器根据命令上下文生成的事件日志。
                eventLogEntryEntityManager.insert(eventHandler.generateEventLogEntry(commandContext), false);
            } catch (Exception e) {
                // 无法创建事件日志
                LOGGER.warn("Could not create event log", e);
            }
        }
    }

    // 在成功刷新会话{@link Session}时调用。当刷新会话期间发生异常时，将不会调用此方法。
    // 如果发生异常且未在此方法中捕获：-将不刷新会话{@link Session}实例，事务上下文{@link TransactionContext}将回滚（如果适用）
    @Override
    public void afterSessionsFlush(CommandContext commandContext) {

    }

    // 关闭失败时调用
    @Override
    public void closeFailure(CommandContext commandContext) {

    }

    // 确定关闭监听器的执行顺序。数值最低者先执行
    @Override
    public Integer order() {
        return 100;
    }

    // 确定此关闭监听器是否允许多次出现
    @Override
    public boolean multipleAllowed() {
        return false;
    }
}
