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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认缓存：将所有内容保留在内存中，除非设置了限制。
 * 用Map结构存储缓存数据
 *
 * @author Joram Barrez
 */
public class DefaultDeploymentCache<T> implements DeploymentCache<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultDeploymentCache.class);

    protected Map<String, T> cache;

    /** 不做限制的缓存
     * 译者注：Collections.synchronizedMap的同步Map方法效率比较低，因为内部使用的是Synchronized锁，推荐使用JUC包下的ConcurrentHashMap实现
     */
    public DefaultDeploymentCache() {
        this.cache = Collections.synchronizedMap(new HashMap<>());
    }

    /**
     * 具有硬限制的缓存：缓存的元素不会超过参数int limit的限制。
     */
    public DefaultDeploymentCache(final int limit) {
        // 译者注：Collections.synchronizedMap的同步Map方法效率比较低，因为内部使用的是Synchronized锁，推荐使用JUC包下的ConcurrentHashMap实现
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, T>(limit + 1, 0.75f, true) { // +1 is needed, because the entry is inserted first, before it is removed
            // 加载因子默认值为0.75（参见javadocs）
            // true将保留“访问顺序”，这是实现LRU缓存算法（即最近最少使用页面置换算法）所需的字段
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, T> eldest) {
                boolean removeEldest = size() > limit;
                if (removeEldest && LOGGER.isTraceEnabled()) {
                    // 已达到缓存限制，{}将被逐出
                    LOGGER.trace("Cache limit is reached, {} will be evicted", eldest.getKey());
                }
                return removeEldest;
            }

        });
    }

    @Override
    public T get(String id) {
        return cache.get(id);
    }

    @Override
    public void add(String id, T obj) {
        cache.put(id, obj);
    }

    @Override
    public void remove(String id) {
        cache.remove(id);
    }

    @Override
    public boolean contains(String id) {
        return cache.containsKey(id);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Collection<T> getAll() {
        return cache.values();
    }

    @Override
    public int size() {
        return cache.size();
    }

}
