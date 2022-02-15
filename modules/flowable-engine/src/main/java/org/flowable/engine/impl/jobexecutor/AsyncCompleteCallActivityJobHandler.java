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
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * 异步完成调用活动作业处理器
 * 异步结束执行的作业处理器{@link JobHandler}实现。
 * 主要用例是处理并行多实例调用活动，其中子进程在到达结束事件之前有一个异步步骤。
 * 异步锁定发生在子进程实例的级别上，但结束事件将执行在范围内完成父进程实例
 * 调用活动的完成回调方法，并可能导致乐观锁定异常。
 * 通过在父进程实例的上下文中调度作业，将使用正确的锁。
 * 
 * @author Joram Barrez
 */
public class AsyncCompleteCallActivityJobHandler implements JobHandler {

    // 类型：异步完成调用活动
    public static final String TYPE = "async-complete-call-actiivty";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {  // the executionId of the job = the parent execution, which will be used for locking
        ExecutionEntity childProcessInstanceExecutionEntity = CommandContextUtil.getExecutionEntityManager(commandContext).findById(configuration); // the child process instance execution
        CommandContextUtil.getAgenda(commandContext).planEndExecutionOperationSynchronous(childProcessInstanceExecutionEntity);
    }

}
