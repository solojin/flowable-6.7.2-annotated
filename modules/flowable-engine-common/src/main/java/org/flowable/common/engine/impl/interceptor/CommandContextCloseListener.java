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
package org.flowable.common.engine.impl.interceptor;

import org.flowable.common.engine.impl.cfg.TransactionContext;

/**
 * 一个监听器，可用于通知命令上下文{@link CommandContext}的生命周期事件。
 * 
 * @author Joram Barrez
 */
public interface CommandContextCloseListener {

    /**
     * 在关闭命令上下文{@link CommandContext}但未执行“关闭逻辑”时调用。
     * 
     * 此时，事务上下文{@link TransactionContext}（如果适用）尚未提交/回滚，会话{@link Session}实例均未刷新。
     * 
     * 如果发生异常且未在此方法中捕获：-将不刷新会话{@link Session}实例，事务上下文{@link TransactionContext}将回滚（如果适用）
     */
    void closing(CommandContext commandContext);

    /**
     * 在成功刷新会话{@link Session}时调用。当刷新会话期间发生异常时，将不会调用此方法。
     * 
     * 如果发生异常且未在此方法中捕获：-将不刷新会话{@link Session}实例，事务上下文{@link TransactionContext}将回滚（如果适用）
     */
    void afterSessionsFlush(CommandContext commandContext);

    /**
     * 当命令上下文{@link CommandContext}成功关闭时调用。
     * 
     * 此时，事务上下文{@link TransactionContext}（如果适用）已成功提交，没有发生回滚。所有会话{@link Session}实例都已关闭。
     * 
     * 请注意，在此处引发异常不会影响事务。命令上下文{@link CommandContext}将记录异常。
     */
    void closed(CommandContext commandContext);

    /**
     * 在命令上下文{@link CommandContext}由于发生异常而未成功关闭时调用。
     * 
     * 请注意，在此处引发异常不会影响事务。命令上下文{@link CommandContext}将记录异常。
     */
    void closeFailure(CommandContext commandContext);
    
    /**
     * 确定关闭监听器的执行顺序
     * 
     * @return 顺序最低者将首先执行
     */
    Integer order();
    
    /**
     * 确定此关闭监听器是否允许多次出现
     * 
     * @return 是否允许多次出现
     */
    boolean multipleAllowed();

}
