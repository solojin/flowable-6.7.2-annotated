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

import java.util.List;

import org.flowable.bpmn.model.Activity;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.behavior.ParallelMultiInstanceBehavior;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ExecutionGraphUtil;
import org.flowable.engine.impl.util.JobUtil;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobService;
import org.flowable.job.service.impl.persistence.entity.JobEntity;
import org.flowable.variable.api.delegate.VariableScope;

/**
 * 无等待状态的并行多实例异步离开作业处理器
 *
 * @author Joram Barrez
 */
public class ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler implements JobHandler {

    public static final String TYPE = "parallel-multi-instance-no-waits-async-leave";

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public void execute(JobEntity job, String configuration, VariableScope variableScope, CommandContext commandContext) {
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration(commandContext);
        ExecutionEntityManager executionEntityManager = processEngineConfiguration.getExecutionEntityManager();

        ExecutionEntity execution = executionEntityManager.findById(job.getExecutionId());
        if (execution != null) {
            FlowElement currentFlowElement = execution.getCurrentFlowElement();
            if (currentFlowElement instanceof Activity) {
                Object behavior = ((Activity) currentFlowElement).getBehavior();
                if (behavior instanceof ParallelMultiInstanceBehavior) {
                    ParallelMultiInstanceBehavior parallelMultiInstanceBehavior = (ParallelMultiInstanceBehavior) behavior;

                    DelegateExecution multiInstanceRootExecution = ExecutionGraphUtil.getMultiInstanceRootExecution(execution);
                    if (multiInstanceRootExecution != null) {

                        // 优化此处的活动计数：如果数据库中仍有活动执行，则无需执行任何操作：
                        // 无需获取任何变量，如已完成的、活动的nr等。
                        // 这项工作可以简单地被重新安排，并且在事情尚未完成时，它将再次尝试相同的逻辑。
                        long activeChildExecutionCount = executionEntityManager.countActiveExecutionsByParentId(multiInstanceRootExecution.getId());
                        if (activeChildExecutionCount > 0) {

                            List<String> boundaryEventActivityIds = ExecutionGraphUtil.getBoundaryEventActivityIds(multiInstanceRootExecution);
                            if (boundaryEventActivityIds.isEmpty()) {
                                reCreateJob(processEngineConfiguration, execution);

                            } else {
                                // 如果所有剩余的执行都是边界事件执行，则可以保留多实例。
                                List<ExecutionEntity> boundaryEventChildExecutions = executionEntityManager
                                    .findExecutionsByParentExecutionAndActivityIds(multiInstanceRootExecution.getId(), boundaryEventActivityIds);
                                if (activeChildExecutionCount <= boundaryEventChildExecutions.size()) {
                                    leaveMultiInstance(processEngineConfiguration, execution, parallelMultiInstanceBehavior);

                                } else {
                                    reCreateJob(processEngineConfiguration, execution);

                                }

                            }

                        } else {
                            leaveMultiInstance(processEngineConfiguration, execution, parallelMultiInstanceBehavior);

                        }
                    }
                }
            }
        }
    }

    protected void reCreateJob(ProcessEngineConfigurationImpl processEngineConfiguration, ExecutionEntity execution) {
        // 这不是创建作业的常见方式，因为我们特别不希望异步执行器触发。
        // 该作业应在下一个采集周期中提取，以避免连续循环。
        JobService jobService = processEngineConfiguration.getJobServiceConfiguration().getJobService();
        JobEntity newJob = JobUtil.createJob(execution, TYPE, processEngineConfiguration);
        jobService.createAsyncJobNoTriggerAsyncExecutor(newJob, true);
        jobService.insertJob(newJob);
    }

    protected void leaveMultiInstance(ProcessEngineConfigurationImpl processEngineConfiguration, ExecutionEntity execution,
        ParallelMultiInstanceBehavior parallelMultiInstanceBehavior) {
        // ParallelMultiInstanceBehavior的实现考虑了子执行。
        // 因此选择随机子执行而不是传递多实例根执行
        boolean multiInstanceCompleted = parallelMultiInstanceBehavior.leaveAsync(execution);
        if (!multiInstanceCompleted) {
            reCreateJob(processEngineConfiguration, execution);
        }
    }


}