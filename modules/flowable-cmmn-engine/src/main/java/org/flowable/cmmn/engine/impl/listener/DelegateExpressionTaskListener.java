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
package org.flowable.cmmn.engine.impl.listener;

import java.util.List;

import org.flowable.cmmn.engine.impl.util.DelegateExpressionUtil;
import org.flowable.cmmn.model.FieldExtension;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.task.service.delegate.DelegateTask;
import org.flowable.task.service.delegate.TaskListener;

/**
 * 负责调度执行delegateExpression方式创建的任务监听器
 *
 * @author Joram Barrez
 */
public class DelegateExpressionTaskListener implements TaskListener {

    protected Expression expression;
    protected List<FieldExtension> fieldExtensions;

    public DelegateExpressionTaskListener(Expression expression, List<FieldExtension> fieldExtensions) {
        this.expression = expression;
        this.fieldExtensions = fieldExtensions;
    }

    @Override
    public void notify(DelegateTask delegateTask) {
        Object delegate = DelegateExpressionUtil.resolveDelegateExpression(expression, delegateTask, fieldExtensions);

        if (delegate instanceof TaskListener) {
            TaskListener taskListener = (TaskListener) delegate;
            taskListener.notify(delegateTask);
        } else {
            // 委托表达式“+ expression +”未解析为任务监听器的实现
            throw new FlowableIllegalArgumentException("Delegate expression " + expression + " did not resolve to an implementation of " + TaskListener.class);
        }
    }

    /**
     * 返回此任务监听器的表达式文本。
     */
    public String getExpressionText() {
        return expression.getExpressionText();
    }

}
