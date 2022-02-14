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
import org.flowable.engine.impl.cmd.ActivateProcessDefinitionCmd;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 定时器激活流程定义处理器
 *
 * @author Joram Barrez
 */
public class TimerActivateProcessDefinitionHandler extends TimerChangeProcessDefinitionSuspensionStateJobHandler {

    // 类型：激活流程定义
    public static final String TYPE = "activate-processdefinition";

    // 获取类型
    @Override
    public String getType() {
        return TYPE;
    }

    // 执行作业，job作业实体，configuration配置，variableScope变量范围，commandContext命令上下文
    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        
        boolean activateProcessInstances = false;
        try {
            JsonNode configNode = processEngineConfiguration.getObjectMapper().readTree(configuration);
            activateProcessInstances = getIncludeProcessInstances(configNode);
        } catch (Exception e) {
            // 读取json值时出错
            throw new FlowableException("Error reading json value " + configuration, e);
        }

        String processDefinitionId = job.getProcessDefinitionId();
        ActivateProcessDefinitionCmd activateProcessDefinitionCmd = new ActivateProcessDefinitionCmd(processDefinitionId, null, activateProcessInstances, null, job.getTenantId());
        activateProcessDefinitionCmd.execute(commandContext);
    }

}
