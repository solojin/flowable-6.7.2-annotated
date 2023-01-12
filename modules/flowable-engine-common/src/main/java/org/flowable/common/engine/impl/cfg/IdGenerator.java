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
package org.flowable.common.engine.impl.cfg;

import org.flowable.common.engine.impl.db.IdBlock;

/**
 * 生成用于为新对象分配ID的｛@link IdBlock｝。
 * 
 * 此类的实例的范围是流程引擎，这意味着一个流程引擎实例中只有一个实例。
 *
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public interface IdGenerator {

    String getNextId();

}
