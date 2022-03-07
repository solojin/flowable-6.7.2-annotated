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
package org.flowable.job.service.impl.asyncexecutor;

import org.flowable.common.engine.impl.cfg.TransactionListener;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.job.api.JobInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 作业添加事务监听器
 *
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class JobAddedTransactionListener implements TransactionListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(JobAddedTransactionListener.class);

    protected JobInfo job;
    protected AsyncExecutor asyncExecutor;
    protected CommandExecutor commandExecutor;

    public JobAddedTransactionListener(JobInfo job, AsyncExecutor asyncExecutor, CommandExecutor commandExecutor) {
        this.job = job;
        this.asyncExecutor = asyncExecutor;
        this.commandExecutor = commandExecutor;
    }

    @Override
    public void execute(CommandContext commandContext) {
        // 无需在新的命令上下文中包装此调用，否则
        // 调用executeAsyncJob需要一个新的数据库连接和事务
        // 这将阻止（调用线程的）当前连接/事务
        // 直到作业被交给异步执行器。
        // 当连接池很小时，这可能会导致争用和（临时）锁定。
        asyncExecutor.executeAsyncJob(job);
    }
}
