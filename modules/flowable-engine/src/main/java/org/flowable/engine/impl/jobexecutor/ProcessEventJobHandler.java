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

package org.flowable.engine.impl.jobexecutor;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.EventSubscriptionUtil;
import org.flowable.eventsubscription.service.EventSubscriptionService;
import org.flowable.eventsubscription.service.impl.persistence.entity.EventSubscriptionEntity;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * 流程事件作业处理器
 *
 * @author Daniel Meyer
 * @author Joram Barrez
 */
public class ProcessEventJobHandler implements JobHandler {

    // 类型：事件
    public static final String TYPE = "event";

    // 获取作业处理器类型
    @Override
    public String getType() {
        return TYPE;
    }

    // 执行作业，job工作实体，configuration配置，variableScope变量范围，commandContext命令上下文
    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        // 根据命令上下文获取流程引擎配置
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        // 根据流程引擎配置获取事件订阅配置以及事件订阅服务类
        EventSubscriptionService eventSubscriptionService = processEngineConfiguration.getEventSubscriptionServiceConfiguration().getEventSubscriptionService();

        // 查找订阅：
        EventSubscriptionEntity eventSubscriptionEntity = eventSubscriptionService.findById(configuration);

        // 如果事件订阅为null，则忽略
        if (eventSubscriptionEntity != null) {
            // 事件订阅工具类，接收事件方法
            EventSubscriptionUtil.eventReceived(eventSubscriptionEntity, null, false);
        }

    }

}
