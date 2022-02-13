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

import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 定时器修改流程定义挂起状态作业处理器
 *
 * @author Joram Barrez
 */
public abstract class TimerChangeProcessDefinitionSuspensionStateJobHandler implements JobHandler {

    // 作业处理器包含的流程实例
    private static final String JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES = "includeProcessInstances";

    // 创建作业处理器配置
    public static String createJobHandlerConfiguration(boolean includeProcessInstances) {
        // 通过命令上下文工具类获取流程引擎配置器
        ObjectNode jsonNode = CommandContextUtil.getProcessEngineConfiguration().getObjectMapper().createObjectNode();
        jsonNode.put(JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES, includeProcessInstances);
        return jsonNode.toString();
    }

    // 获取标志位，是否包含流程实例
    public static boolean getIncludeProcessInstances(JsonNode configNode) {
        // 从配置节点中获取属性：JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES 作业处理器包含的流程实例
        if (configNode.has(JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES)) {
            return configNode.get(JOB_HANDLER_CFG_INCLUDE_PROCESS_INSTANCES).asBoolean();
        }
        // 默认返回false
        return false;
    }

}
