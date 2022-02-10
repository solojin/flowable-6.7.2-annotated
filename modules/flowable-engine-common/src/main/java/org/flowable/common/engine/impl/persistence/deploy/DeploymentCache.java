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
package org.flowable.common.engine.impl.persistence.deploy;

import java.util.Collection;

/**
 * 缓存实现的接口类。
 * 泛型参数
 * 
 * @author Joram Barrez
 */
public interface DeploymentCache<T> {

    // 根据ID获取缓存实体
    T get(String id);

    // 根据ID判断是否包含
    boolean contains(String id);

    // 根据ID新的缓存实体
    void add(String id, T object);

    // 根据ID移除缓存实体
    void remove(String id);

    // 清除全部缓存
    void clear();

    // 获取所有缓存实体
    Collection<T> getAll();

    // 获取缓存实体的数量
    int size();
}
