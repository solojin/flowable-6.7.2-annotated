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
package org.flowable.engine.delegate;

/**
 * 回调接口，用于通知执行事件，如启动流程实例、结束活动实例或进行转换。
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 * @author Yvo Swillens
 */
public interface ExecutionListener extends BaseExecutionListener {

    void notify(DelegateExecution execution);
}
