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

package org.activiti.spring;

import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.springframework.core.Ordered;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 将事务交给Spring管理
 *
 * @author Frederik Heremans
 * @author Joram Barrez
 */
public class SpringTransactionContext implements TransactionContext {

    protected PlatformTransactionManager transactionManager;
    protected CommandContext commandContext;
    protected Integer transactionSynchronizationAdapterOrder;

    public SpringTransactionContext(PlatformTransactionManager transactionManager, CommandContext commandContext) {
        this(transactionManager, commandContext, null);
    }

    public SpringTransactionContext(PlatformTransactionManager transactionManager, CommandContext commandContext, Integer transactionSynchronizationAdapterOrder) {
        this.transactionManager = transactionManager;
        this.commandContext = commandContext;
        if (transactionSynchronizationAdapterOrder != null) {
            this.transactionSynchronizationAdapterOrder = transactionSynchronizationAdapterOrder;
        } else {
            // 恢复到默认值，这是一个很高的数字，因为添加订单之前的行为会将TransactionSynchronizationAdapter
            // 设置为在所有实现Ordered的适配器之后调用
            this.transactionSynchronizationAdapterOrder = Integer.MAX_VALUE;
        }
    }

    @Override
    public void commit() {
        // 什么都不做，事务由spring管理
    }

    @Override
    public void rollback() {
        // 为了防止回滚不是由异常触发的，我们将当前事务标记为rollBackOnly。
        transactionManager.getTransaction(null).setRollbackOnly();
    }

    @Override
    public void addTransactionListener(final TransactionState transactionState, final TransactionListener transactionListener) {
        if (transactionState == TransactionState.COMMITTING) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void beforeCommit(boolean readOnly) {
                    transactionListener.execute(commandContext);
                }
            });

        } else if (transactionState == TransactionState.COMMITTED) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCommit() {
                    transactionListener.execute(commandContext);
                }
            });

        } else if (transactionState == TransactionState.ROLLINGBACK) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void beforeCompletion() {
                    transactionListener.execute(commandContext);
                }
            });

        } else if (transactionState == TransactionState.ROLLED_BACK) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void afterCompletion(int status) {
                    if (TransactionSynchronization.STATUS_ROLLED_BACK == status) {
                        transactionListener.execute(commandContext);
                    }
                }
            });
        }
    }

    protected abstract class TransactionSynchronizationAdapter implements TransactionSynchronization, Ordered {

        @Override
        public void suspend() {
        }

        @Override
        public void resume() {
        }

        @Override
        public void flush() {
        }

        @Override
        public void beforeCommit(boolean readOnly) {
        }

        @Override
        public void beforeCompletion() {
        }

        @Override
        public void afterCommit() {
        }

        @Override
        public void afterCompletion(int status) {
        }

        @Override
        public int getOrder() {
            return transactionSynchronizationAdapterOrder;
        }

    }

}
