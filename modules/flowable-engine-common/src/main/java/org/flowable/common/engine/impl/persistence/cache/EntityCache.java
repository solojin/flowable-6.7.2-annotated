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
package org.flowable.common.engine.impl.persistence.cache;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.persistence.entity.Entity;

/**
 * 这是在执行一个命令{@link命令}期间实体{@link Entity}实例的缓存.
 * 
 * @author Joram Barrez
 */
public interface EntityCache extends Session {

    /**
     * 以Map映射的形式返回所有缓存的实体{@link Entity}实例，其结构如下: { entityClassName, {entityId, entity} }
     */
    Map<Class<?>, Map<String, CachedEntity>> getAllCachedEntities();

    /**
     * 将实体{@link Entity}添加到缓存中。
     * 
     * @param entity
     *             实体{@link Entity} 实例
     * @param storeState
     *            如果为true，将存储当前状态{@link Entity#getPersistentState（）}，以备将来进行区分。请注意，如果为false，实体{@link Entity}将始终被视为已更改。
     * @return 返回一个缓存实体{@link CachedEntity}实例，稍后可以对其进行充实。
     */
    CachedEntity put(Entity entity, boolean storeState);

    /**
     * 使用提供的id返回给定类的缓存实体{@link Entity}实例。如果找不到这样的实体{@link Entity}，则返回null。
     */
    <T> T findInCache(Class<T> entityClass, String id);

    /**
     * 返回给定类型的所有缓存的实体{@link Entity}实例。 如果不存在给定类型的实例，则返回空列表。
     */
    <T> List<T> findInCache(Class<T> entityClass);

    /**
     * 返回给定类型的所有实体{@ link cachedential }实例。
     * 与{@link#findInCache（Class）}的不同之处在于，这里返回整个缓存实体{@link CachedEntity}，它允许访问将其放入缓存时的持久状态。
     */
    <T> Collection<CachedEntity> findInCacheAsCachedObjects(Class<T> entityClass);

    /**
     * 从缓存中删除具有给定id的给定类型的实体{@link Entity}。
     */
    void cacheRemove(Class<?> entityClass, String entityId);
}
