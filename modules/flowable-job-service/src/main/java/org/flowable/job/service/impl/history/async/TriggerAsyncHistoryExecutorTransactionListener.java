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
package org.flowable.job.service.impl.history.async;

import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.persistence.entity.HistoryJobEntity;

/**
 * 触发器异步历史执行器事务监听器
 * 一个事务监听器{@link TransactionListener}，通常在提交后触发
 * 异步历史执行器，用于执行提供的历史作业实体{@link HistoryJobEntity}实例列表。
 * 
 * @author Joram Barrez
 */
public class TriggerAsyncHistoryExecutorTransactionListener implements TransactionListener {
    
    protected HistoryJobEntity historyJobEntity;
    
    protected JobServiceConfiguration jobServiceConfiguration;
    
    public TriggerAsyncHistoryExecutorTransactionListener(JobServiceConfiguration jobServiceConfiguration, HistoryJobEntity historyJobEntity) {
        // 此侦听器的执行将引用可能命令上下文关闭时不可用（通常在历史记录作业已创建并计划），因此它们已在此处引用。
        this.jobServiceConfiguration = jobServiceConfiguration;
        this.historyJobEntity = historyJobEntity;
    }
    
    @Override
    public void execute(CommandContext commandContext) {
        jobServiceConfiguration.getAsyncHistoryExecutor().executeAsyncJob(historyJobEntity);
    }

}
