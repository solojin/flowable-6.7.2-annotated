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
package org.activiti.engine.impl.history.handler;

import org.activiti.engine.impl.context.Context;
import org.activiti.engine.impl.persistence.entity.TaskEntity;
import org.flowable.engine.delegate.TaskListener;
import org.flowable.task.service.delegate.DelegateTask;

/**
 * 为用户任务活动创建任务时调用。允许在历史活动中记录任务id。
 *
 * 负责更新在历史活动表ACT_HI_ACTINST中的任务ID值
 * @author Frederik Heremans
 */
public class UserTaskIdHandler implements TaskListener {

    @Override
    public void notify(DelegateTask task) {
        Context.getCommandContext().getHistoryManager()
                .recordTaskId((TaskEntity) task);
    }

}
