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
package org.flowable.engine;

import org.flowable.common.engine.api.Engine;
import org.flowable.common.engine.impl.FlowableVersions;

/**
 * 流程引擎 接口类
 * 提供对暴露BPM和工作流操作的所有服务的访问。
 * {@link org.flowable.engine.RuntimeService}: 允许创建流程运行时实例和查找流程实例 {@link org.flowable.engine.repository.Deployment}s {@link org.flowable.engine.runtime.ProcessInstance}s.
 * {@link org.flowable.engine.TaskService}: 暴露给管理人的操作 (单线程) {@link org.flowable.task.api.Task}s, 比如申领、完成和指派任务
 * {@link org.flowable.engine.IdentityService}: 用来管理用户、组，还有用户和组之间关系的服务
 * {@link org.flowable.engine.ManagementService}: 暴露引擎管理和维护的操作
 * {@link org.flowable.engine.HistoryService}: 暴露历史实例信息的服务
 * 通常，最终用户应用程序中只需要一个中央ProcessEngine实例。构建ProcessEngine是通过{@link ProcessEngineConfiguration}实例完成的，是一个
 * 应避免的昂贵操作。为此，建议将其存储在静态字段或JNDI位置（或类似位置）。那是一个线程安全的对象，因此没有特殊的需要采取的预防措施。
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface ProcessEngine extends Engine {

    /** Flowable库的版本 */
    String VERSION = FlowableVersions.CURRENT_VERSION;

    /**
     * 启动的执行方法（异步或历史异步），可配置为自动激活。
     */
    void startExecutors();

    RepositoryService getRepositoryService();

    RuntimeService getRuntimeService();

    FormService getFormService();

    TaskService getTaskService();

    HistoryService getHistoryService();

    IdentityService getIdentityService();

    ManagementService getManagementService();

    DynamicBpmnService getDynamicBpmnService();

    ProcessMigrationService getProcessMigrationService();

    ProcessEngineConfiguration getProcessEngineConfiguration();
}
