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
package org.activiti.engine.impl.jobexecutor;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.el.NoExecutionVariableScope;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.TimerJobEntity;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.calendar.BusinessCalendar;
import org.flowable.engine.impl.jobexecutor.TimerDeclarationType;
import org.flowable.variable.api.delegate.VariableScope;
import org.joda.time.DateTime;

/**
 * 定时器声明实现类
 *
 * @author Tom Baeyens
 */
public class TimerDeclarationImpl implements Serializable {

    private static final long serialVersionUID = 1L;

    // 表达式描述
    protected Expression description;
    // 定时器声明类型
    protected TimerDeclarationType type;
    // 截止日期表达式
    protected Expression endDateExpression;
    // 日程表名称表达式
    protected Expression calendarNameExpression;

    // 作业处理器类型
    protected String jobHandlerType;
    // 作业处理器配置
    protected String jobHandlerConfiguration;
    protected String repeat;
    protected boolean exclusive = TimerJobEntity.DEFAULT_EXCLUSIVE;
    protected int retries = TimerJobEntity.DEFAULT_RETRIES;
    protected boolean isInterruptingTimer; // 边界定时器

    public TimerDeclarationImpl(Expression expression, TimerDeclarationType type, String jobHandlerType, Expression endDateExpression, Expression calendarNameExpression) {
        this(expression, type, jobHandlerType);
        this.endDateExpression = endDateExpression;
        this.calendarNameExpression = calendarNameExpression;
    }

    public TimerDeclarationImpl(Expression expression, TimerDeclarationType type, String jobHandlerType) {
        this.jobHandlerType = jobHandlerType;
        this.description = expression;
        this.type = type;
    }

    public Expression getDescription() {
        return description;
    }

    public String getJobHandlerType() {
        return jobHandlerType;
    }

    public String getJobHandlerConfiguration() {
        return jobHandlerConfiguration;
    }

    public void setJobHandlerConfiguration(String jobHandlerConfiguration) {
        this.jobHandlerConfiguration = jobHandlerConfiguration;
    }

    public String getRepeat() {
        return repeat;
    }

    public void setRepeat(String repeat) {
        this.repeat = repeat;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public void setJobHandlerType(String jobHandlerType) {
        this.jobHandlerType = jobHandlerType;
    }

    public boolean isInterruptingTimer() {
        return isInterruptingTimer;
    }

    public void setInterruptingTimer(boolean isInterruptingTimer) {
        this.isInterruptingTimer = isInterruptingTimer;
    }

    public TimerJobEntity prepareTimerEntity(ExecutionEntity executionEntity) {
        // ACT-1415:启动事件的计时器声明可能包含表达式NOT
        //评估变量但在其他情况下，评估仍应进行
        VariableScope scopeForExpression = executionEntity;
        if (scopeForExpression == null) {
            scopeForExpression = NoExecutionVariableScope.getSharedInstance();
        }

        String calendarNameValue = type.calendarName;
        if (this.calendarNameExpression != null) {
            calendarNameValue = (String) this.calendarNameExpression.getValue(scopeForExpression);
        }

        BusinessCalendar businessCalendar = Context
                .getProcessEngineConfiguration()
                .getBusinessCalendarManager()
                .getBusinessCalendar(calendarNameValue);

        if (description == null) {
            // 防止下一行发生NPE
            throw new ActivitiIllegalArgumentException("Timer '" + executionEntity.getActivityId() + "' was not configured with a valid duration/time");
        }

        String endDateString = null;
        String dueDateString = null;
        Date duedate = null;
        Date endDate = null;

        if (endDateExpression != null && !(scopeForExpression instanceof NoExecutionVariableScope)) {
            Object endDateValue = endDateExpression.getValue(scopeForExpression);
            if (endDateValue instanceof String) {
                endDateString = (String) endDateValue;
            } else if (endDateValue instanceof Date) {
                endDate = (Date) endDateValue;
            } else if (endDateValue instanceof DateTime) {
                // Joda日期时间支持
                duedate = ((DateTime) endDateValue).toDate();
            } else {
                throw new ActivitiException("Timer '" + executionEntity.getActivityId() + "' was not configured with a valid duration/time, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
            }

            if (endDate == null) {
                endDate = businessCalendar.resolveEndDate(endDateString);
            }
        }

        Object dueDateValue = description.getValue(scopeForExpression);
        if (dueDateValue instanceof String) {
            dueDateString = (String) dueDateValue;
        } else if (dueDateValue instanceof Date) {
            duedate = (Date) dueDateValue;
        } else if (dueDateValue instanceof DateTime) {
            // Joda日期时间支持
            duedate = ((DateTime) dueDateValue).toDate();
        } else if (dueDateValue != null) {
            // dueDateValue==null是可以的，但意外的类类型必须抛出错误。
            throw new ActivitiException("Timer '" + executionEntity.getActivityId() + "' was not configured with a valid duration/time, either hand in a java.util.Date or a String in format 'yyyy-MM-dd'T'hh:mm:ss'");
        }

        if (duedate == null && dueDateString != null) {
            duedate = businessCalendar.resolveDuedate(dueDateString);
        }

        TimerJobEntity timer = null;
        // 如果dueDateValue为空->这是可以的-定时器将为空，作业未预定
        if (duedate != null) {
            timer = new TimerJobEntity(this);
            timer.setDuedate(duedate);
            timer.setEndDate(endDate);

            if (executionEntity != null) {
                timer.setExecution(executionEntity);
                timer.setProcessDefinitionId(executionEntity.getProcessDefinitionId());
                timer.setProcessInstanceId(executionEntity.getProcessInstanceId());

                // 继承租户标识符（如果适用）
                if (executionEntity.getTenantId() != null) {
                    timer.setTenantId(executionEntity.getTenantId());
                }
            }

            if (type == TimerDeclarationType.CYCLE) {

                // 参见ACT-1427：带有cancelActivity='true'的边界定时器不需要重复自身
                boolean repeat = !isInterruptingTimer;

                // ACT-1951：根据规范，中间捕获定时器事件不应重复
                if (TimerCatchIntermediateEventJobHandler.TYPE.equals(jobHandlerType)) {
                    repeat = false;
                    if (endDate != null) {
                        long endDateMiliss = endDate.getTime();
                        long dueDateMiliss = duedate.getTime();
                        long dueDate = Math.min(endDateMiliss, dueDateMiliss);
                        timer.setDuedate(new Date(dueDate));
                    }
                }

                if (repeat) {
                    String prepared = prepareRepeat(dueDateString);
                    timer.setRepeat(prepared);
                }
            }
        }
        return timer;
    }

    private String prepareRepeat(String dueDate) {
        if (dueDate.startsWith("R") && dueDate.split("/").length == 2) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            return dueDate.replace("/", "/" + sdf.format(Context.getProcessEngineConfiguration().getClock().getCurrentTime()) + "/");
        }
        return dueDate;
    }
}
