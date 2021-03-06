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
package org.activiti.engine.impl.cfg.standalone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.impl.cfg.TransactionContext;
import org.activiti.engine.impl.cfg.TransactionListener;
import org.activiti.engine.impl.cfg.TransactionState;
import org.activiti.engine.impl.db.DbSqlSession;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.interceptor.CommandConfig;
import org.activiti.engine.impl.interceptor.CommandContext;
import org.activiti.engine.impl.interceptor.CommandExecutor;
import org.apache.ibatis.session.SqlSession;
import org.flowable.common.engine.impl.cfg.TransactionPropagation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 独立MyBatis事务上下文
 * 最终委托SqlSession实例对象完成事务管理
 *
 * @author Tom Baeyens
 */
public class StandaloneMybatisTransactionContext implements TransactionContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandaloneMybatisTransactionContext.class);

    protected CommandContext commandContext;
    protected Map<TransactionState, List<TransactionListener>> stateTransactionListeners;

    public StandaloneMybatisTransactionContext(CommandContext commandContext) {
        this.commandContext = commandContext;
    }

    @Override
    public void addTransactionListener(TransactionState transactionState, TransactionListener transactionListener) {
        if (stateTransactionListeners == null) {
            stateTransactionListeners = new HashMap<>();
        }
        List<TransactionListener> transactionListeners = stateTransactionListeners.get(transactionState);
        if (transactionListeners == null) {
            transactionListeners = new ArrayList<>();
            stateTransactionListeners.put(transactionState, transactionListeners);
        }
        transactionListeners.add(transactionListener);
    }

    @Override
    public void commit() {

        LOGGER.debug("firing event committing...");
        fireTransactionEvent(TransactionState.COMMITTING, false);

        LOGGER.debug("committing the ibatis sql session...");
        getDbSqlSession().commit();
        LOGGER.debug("firing event committed...");
        fireTransactionEvent(TransactionState.COMMITTED, true);

    }

    /**
     * 为提供的事务{@link TransactionState}激发事件。
     *
     * @param transactionState    将为其调用监听器的事务状态{@link TransactionState}。
     * @param executeInNewContext 如果为true，将在新的命令上下文中调用侦听器。例如，当触发{@link TransactionState#COMMITTED}事件时，
     * 就需要这样做：事务已经完成在同一上下文中提交和执行逻辑可能会导致奇怪的行为（例如，
     * 执行{@link SqlSession#update（String）}）实际上会回滚更新（正如MyBatis上下文已提交，内部标志未正确设置）。
     */
    protected void fireTransactionEvent(TransactionState transactionState, boolean executeInNewContext) {
        if (stateTransactionListeners == null) {
            return;
        }
        final List<TransactionListener> transactionListeners = stateTransactionListeners.get(transactionState);
        if (transactionListeners == null) {
            return;
        }

        if (executeInNewContext) {
            CommandExecutor commandExecutor = commandContext.getProcessEngineConfiguration().getCommandExecutor();
            CommandConfig commandConfig = new CommandConfig(false, TransactionPropagation.REQUIRES_NEW);
            commandExecutor.execute(commandConfig, new Command<Void>() {
                @Override
                public Void execute(CommandContext commandContext) {
                    executeTransactionListeners(transactionListeners, commandContext);
                    return null;
                }
            });
        } else {
            executeTransactionListeners(transactionListeners, commandContext);
        }

    }

    protected void executeTransactionListeners(List<TransactionListener> transactionListeners, CommandContext commandContext) {
        for (TransactionListener transactionListener : transactionListeners) {
            transactionListener.execute(commandContext);
        }
    }

    protected DbSqlSession getDbSqlSession() {
        return commandContext.getSession(DbSqlSession.class);
    }

    @Override
    public void rollback() {
        try {
            try {
                LOGGER.debug("firing event rolling back...");
                fireTransactionEvent(TransactionState.ROLLINGBACK, false);

            } catch (Throwable exception) {
                LOGGER.info("Exception during transaction: {}", exception.getMessage());
                commandContext.exception(exception);
            } finally {
                LOGGER.debug("rolling back ibatis sql session...");
                getDbSqlSession().rollback();
            }

        } catch (Throwable exception) {
            LOGGER.info("Exception during transaction: {}", exception.getMessage());
            commandContext.exception(exception);

        } finally {
            LOGGER.debug("firing event rolled back...");
            fireTransactionEvent(TransactionState.ROLLED_BACK, true);
        }
    }
}
