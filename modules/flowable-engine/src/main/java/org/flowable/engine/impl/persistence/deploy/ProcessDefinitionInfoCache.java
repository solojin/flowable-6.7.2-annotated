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
package org.flowable.engine.impl.persistence.deploy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.context.Context;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandExecutor;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * 默认缓存：将所有内容保留在内存中，除非设置了限制。
 * 用Map结构存储缓存信息，缓存实体为ProcessDefinitionInfoCacheObject流程定义缓存对象
 * 
 * @author Tijs Rademakers
 */
public class ProcessDefinitionInfoCache implements DeploymentCache<ProcessDefinitionInfoCacheObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessDefinitionInfoCache.class);

    protected Map<String, ProcessDefinitionInfoCacheObject> cache;
    protected CommandExecutor commandExecutor;

    /** 不做限制的缓存 */
    public ProcessDefinitionInfoCache(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
        // 译者注：Collections.synchronizedMap的同步Map方法效率比较低，因为内部使用的是Synchronized锁，推荐使用JUC包下的ConcurrentHashMap实现
        this.cache = Collections.synchronizedMap(new HashMap<>());
    }

    /** 具有硬限制的缓存：缓存的元素数量不会超过int limit该限制。 */
    public ProcessDefinitionInfoCache(CommandExecutor commandExecutor, final int limit) {
        this.commandExecutor = commandExecutor;
        // 译者注：Collections.synchronizedMap的同步Map方法效率比较低，因为内部使用的是Synchronized锁，推荐使用JUC包下的ConcurrentHashMap实现
        this.cache = Collections.synchronizedMap(new LinkedHashMap<String, ProcessDefinitionInfoCacheObject>(limit + 1, 0.75f, true) {
            // +1是必需的，因为在删除条目之前先插入该条目
            // 加载因子默认值为0.75（参见javadocs）
            // true将保留“访问顺序”，这是实现LRU缓存算法（即最近最少使用页面置换算法）所需的字段
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<String, ProcessDefinitionInfoCacheObject> eldest) {
                boolean removeEldest = size() > limit;
                if (removeEldest) {
                    // 已达到缓存限制，{}将被逐出
                    LOGGER.trace("Cache limit is reached, {} will be evicted", eldest.getKey());
                }
                return removeEldest;
            }

        });
    }

    @Override
    public ProcessDefinitionInfoCacheObject get(final String processDefinitionId) {
        ProcessDefinitionInfoCacheObject infoCacheObject = null;
        Command<ProcessDefinitionInfoCacheObject> cacheCommand = new Command<ProcessDefinitionInfoCacheObject>() {

            @Override
            public ProcessDefinitionInfoCacheObject execute(CommandContext commandContext) {
                return retrieveProcessDefinitionInfoCacheObject(processDefinitionId, commandContext);
            }
        };

        if (Context.getCommandContext() != null) {
            infoCacheObject = retrieveProcessDefinitionInfoCacheObject(processDefinitionId, Context.getCommandContext());
        } else {
            infoCacheObject = commandExecutor.execute(cacheCommand);
        }

        return infoCacheObject;
    }

    @Override
    public boolean contains(String id) {
        return cache.containsKey(id);
    }

    @Override
    public void add(String id, ProcessDefinitionInfoCacheObject obj) {
        cache.put(id, obj);
    }

    @Override
    public void remove(String id) {
        cache.remove(id);
    }

    @Override
    public void clear() {
        cache.clear();
    }

    @Override
    public Collection<ProcessDefinitionInfoCacheObject> getAll() {
        return cache.values();
    }

    @Override
    public int size() {
        return cache.size();
    }

    protected ProcessDefinitionInfoCacheObject retrieveProcessDefinitionInfoCacheObject(String processDefinitionId, CommandContext commandContext) {
        ProcessDefinitionInfoEntityManager infoEntityManager = CommandContextUtil.getProcessDefinitionInfoEntityManager(commandContext);
        ObjectMapper objectMapper = CommandContextUtil.getProcessEngineConfiguration(commandContext).getObjectMapper();

        ProcessDefinitionInfoCacheObject cacheObject = null;
        if (cache.containsKey(processDefinitionId)) {
            cacheObject = cache.get(processDefinitionId);
        } else {
            cacheObject = new ProcessDefinitionInfoCacheObject();
            cacheObject.setRevision(0);
            cacheObject.setInfoNode(objectMapper.createObjectNode());
        }

        ProcessDefinitionInfoEntity infoEntity = infoEntityManager.findProcessDefinitionInfoByProcessDefinitionId(processDefinitionId);
        if (infoEntity != null && infoEntity.getRevision() != cacheObject.getRevision()) {
            cacheObject.setRevision(infoEntity.getRevision());
            if (infoEntity.getInfoJsonId() != null) {
                byte[] infoBytes = infoEntityManager.findInfoJsonById(infoEntity.getInfoJsonId());
                try {
                    ObjectNode infoNode = (ObjectNode) objectMapper.readTree(infoBytes);
                    cacheObject.setInfoNode(infoNode);
                } catch (Exception e) {
                    // 读取进程定义的json info节点时出错
                    throw new FlowableException("Error reading json info node for process definition " + processDefinitionId, e);
                }
            }
        } else if (infoEntity == null) {
            cacheObject.setRevision(0);
            cacheObject.setInfoNode(objectMapper.createObjectNode());
        }

        return cacheObject;
    }

}
