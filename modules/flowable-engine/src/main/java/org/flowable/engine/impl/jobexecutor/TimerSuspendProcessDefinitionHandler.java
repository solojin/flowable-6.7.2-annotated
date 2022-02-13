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

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.cmd.SuspendProcessDefinitionCmd;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 定时器挂起流程定义处理器
 * 继承定时器修改流程定义挂起状态作业处理器
 *
 * @author Joram Barrez
 */
public class TimerSuspendProcessDefinitionHandler extends TimerChangeProcessDefinitionSuspensionStateJobHandler {

    // 类型：挂起流程定义
    public static final String TYPE = "suspend-processdefinition";

    @Override
    public String getType() {
        return TYPE;
    }

    // 执行作业，job作业实体，configuration配置，variableScope变量范围，commandContext命令上下文
    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        // 根据命令上下文，通过命令上下文工具类获取流程引擎配置
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);

        // 挂起流程实例标志位，默认false
        boolean suspendProcessInstances = false;
        try {
            // 从流程引擎配置器获取配置节点
            JsonNode configNode = processEngineConfiguration.getObjectMapper().readTree(configuration);
            // 获取标志位，是否包含流程实例
            suspendProcessInstances = getIncludeProcessInstances(configNode);
        } catch (Exception e) {
            // 读取json值时出错
            throw new FlowableException("Error reading json value " + configuration, e);
        }

        // 从作业实体获取流程定义ID
        String processDefinitionId = job.getProcessDefinitionId();

        // 新建挂起流程定义命令
        SuspendProcessDefinitionCmd suspendProcessDefinitionCmd = new SuspendProcessDefinitionCmd(processDefinitionId, null, suspendProcessInstances, null, job.getTenantId());
        // 执行挂起流程定义命令
        suspendProcessDefinitionCmd.execute(commandContext);
    }

}
