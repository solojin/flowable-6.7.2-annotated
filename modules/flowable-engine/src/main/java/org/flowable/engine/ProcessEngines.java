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
package org.flowable.engine;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.common.engine.impl.util.IoUtil;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 流程引擎抽象类
 *
 * 在服务器环境下，初始化和关闭流程引擎的助手
 * 所有创建的流程引擎类{@link ProcessEngine}都将注册到此类
 * 通过一个上下文监听器context-listener ( org.flowable.impl.servlet.listener.ProcessEnginesServletContextListener )，当webapp部署时，会调用初始化方法{@link #init()};当webapp销毁时，会调用销毁方法{@link #destroy()}
 * 这样的话，所用应用都可以仅仅使用流程引擎抽象类去获得预初始和缓存的流程引擎。
 * 请注意，流程引擎没有懒加载，所以确保上下文监听器已经配置或流程引擎接口类{@link ProcessEngine}已经被创建且注册到此类。
 * 初始化方法{@link #init()}将会为在classpath目录下的每一个flowable.cfg.xml文件创建一个流程引擎接口类{@link ProcessEngine}。
 * 如果你有不只一个流程引擎配置文件，请确保它们有不同的名称。
 * 
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class ProcessEngines {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngines.class);

    public static final String NAME_DEFAULT = "default";

    protected static boolean isInitialized;
    protected static Map<String, ProcessEngine> processEngines = new HashMap<>();
    protected static Map<String, EngineInfo> processEngineInfosByName = new HashMap<>();
    protected static Map<String, EngineInfo> processEngineInfosByResourceUrl = new HashMap<>();
    protected static List<EngineInfo> processEngineInfos = new ArrayList<>();

    /**
     * 初始化所有能在classpath文件夹下找到的资源文件
     * Flowable 风格的资源文件 flowable.cfg.xml
     * Spring 风格的资源文件 flowable-context.xml
     */
    public static synchronized void init() {
        if (!isInitialized()) {
            if (processEngines == null) {
                // 当前map为空时，创建一个新的map去存放流程引擎
                processEngines = new HashMap<>();
            }
            ClassLoader classLoader = ReflectUtil.getClassLoader();
            Enumeration<URL> resources = null;
            try {
                resources = classLoader.getResources("flowable.cfg.xml");
            } catch (IOException e) {
                throw new FlowableIllegalArgumentException("problem retrieving flowable.cfg.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }

            // 使用set集合去重配置路径。
            // 一些类加载器可能会返回两次完全相同的路径，造成重复启动。
            Set<URL> configUrls = new HashSet<>();
            while (resources.hasMoreElements()) {
                configUrls.add(resources.nextElement());
            }
            for (URL resource : configUrls) {
                LOGGER.info("Initializing process engine using configuration '{}'", resource);
                initProcessEngineFromResource(resource);
            }

            try {
                resources = classLoader.getResources("flowable-context.xml");
            } catch (IOException e) {
                throw new FlowableIllegalArgumentException("problem retrieving flowable-context.xml resources on the classpath: " + System.getProperty("java.class.path"), e);
            }
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                LOGGER.info("Initializing process engine using Spring configuration '{}'", resource);
                initProcessEngineFromSpringResource(resource);
            }

            setInitialized(true);
        } else {
            LOGGER.info("Process engines already initialized");
        }
    }

    protected static void initProcessEngineFromSpringResource(URL resource) {
        try {
            Class<?> springConfigurationHelperClass = ReflectUtil.loadClass("org.flowable.spring.SpringConfigurationHelper");
            Method method = springConfigurationHelperClass.getDeclaredMethod("buildProcessEngine", new Class<?>[] { URL.class });
            ProcessEngine processEngine = (ProcessEngine) method.invoke(null, new Object[] { resource });

            String processEngineName = processEngine.getName();
            EngineInfo processEngineInfo = new EngineInfo(processEngineName, resource.toString(), null);
            processEngineInfosByName.put(processEngineName, processEngineInfo);
            processEngineInfosByResourceUrl.put(resource.toString(), processEngineInfo);

        } catch (Exception e) {
            throw new FlowableException("couldn't initialize process engine from spring configuration resource " + resource + ": " + e.getMessage(), e);
        }
    }

    /**
     * 注册已经提供的流程引擎。
     * 没有引擎信息{@link EngineInfo}可用于此进程引擎。
     * 调用销毁方法{@link ProcessEngines#destroy()}可关闭已经注册的引擎。*
     */
    public static void registerProcessEngine(ProcessEngine processEngine) {
        processEngines.put(processEngine.getName(), processEngine);
    }

    /**
     * 移除一个已注册的流程引擎
     */
    public static void unregister(ProcessEngine processEngine) {
        processEngines.remove(processEngine.getName());
    }

    private static EngineInfo initProcessEngineFromResource(URL resourceUrl) {
        EngineInfo processEngineInfo = processEngineInfosByResourceUrl.get(resourceUrl.toString());
        // 如果这里存在流程引擎信息
        if (processEngineInfo != null) {
            // 从成员字段中删除该流程引擎
            processEngineInfos.remove(processEngineInfo);
            if (processEngineInfo.getException() == null) {
                String processEngineName = processEngineInfo.getName();
                processEngines.remove(processEngineName);
                processEngineInfosByName.remove(processEngineName);
            }
            processEngineInfosByResourceUrl.remove(processEngineInfo.getResourceUrl());
        }

        String resourceUrlString = resourceUrl.toString();
        try {
            LOGGER.info("initializing process engine for resource {}", resourceUrl);
            ProcessEngine processEngine = buildProcessEngine(resourceUrl);
            String processEngineName = processEngine.getName();
            LOGGER.info("initialised process engine {}", processEngineName);
            processEngineInfo = new EngineInfo(processEngineName, resourceUrlString, null);
            processEngines.put(processEngineName, processEngine);
            processEngineInfosByName.put(processEngineName, processEngineInfo);
        } catch (Throwable e) {
            LOGGER.error("Exception while initializing process engine: {}", e.getMessage(), e);
            processEngineInfo = new EngineInfo(null, resourceUrlString, ExceptionUtils.getStackTrace(e));
        }
        processEngineInfosByResourceUrl.put(resourceUrlString, processEngineInfo);
        processEngineInfos.add(processEngineInfo);
        return processEngineInfo;
    }

    private static ProcessEngine buildProcessEngine(URL resource) {
        InputStream inputStream = null;
        try {
            inputStream = resource.openStream();
            ProcessEngineConfiguration processEngineConfiguration = ProcessEngineConfiguration.createProcessEngineConfigurationFromInputStream(inputStream);
            return processEngineConfiguration.buildProcessEngine();

        } catch (IOException e) {
            throw new FlowableIllegalArgumentException("couldn't open resource stream: " + e.getMessage(), e);
        } finally {
            IoUtil.closeSilently(inputStream);
        }
    }

    /** 获取初始化的结果集。 */
    public static List<EngineInfo> getProcessEngineInfos() {
        return processEngineInfos;
    }

    /**
     * 获取初始化的结果集。
     * 只能获取到通过初始化方法{@link ProcessEngines#init()}增加的流程引擎。
     * 获取不到通过流程方式注册的流程引擎。
     */
    public static EngineInfo getProcessEngineInfo(String processEngineName) {
        return processEngineInfosByName.get(processEngineName);
    }

    public static ProcessEngine getDefaultProcessEngine() {
        return getProcessEngine(NAME_DEFAULT);
    }

    /**
     * 通过一个名称获取流程引擎
     * 
     * @param processEngineName
     *            一个流程引擎的名称或者为空时使用默认的流程引擎。
     */
    public static ProcessEngine getProcessEngine(String processEngineName) {
        if (!isInitialized()) {
            init();
        }
        return processEngines.get(processEngineName);
    }

    /**
     * 重新初始化先前失败的流程引擎
     */
    public static EngineInfo retry(String resourceUrl) {
        LOGGER.debug("retying initializing of resource {}", resourceUrl);
        try {
            return initProcessEngineFromResource(new URL(resourceUrl));
        } catch (MalformedURLException e) {
            throw new FlowableIllegalArgumentException("invalid url: " + resourceUrl, e);
        }
    }

    /**
     * 在托管服务器环境下，向应用程序客户端提供对流程引擎的访问。
     */
    public static Map<String, ProcessEngine> getProcessEngines() {
        return processEngines;
    }

    /**
     * 关闭所有的流程引擎。
     * 应该在服务器关闭时调用此方法。
     */
    public static synchronized void destroy() {
        if (isInitialized()) {
            Map<String, ProcessEngine> engines = new HashMap<>(processEngines);
            processEngines = new HashMap<>();

            for (String processEngineName : engines.keySet()) {
                ProcessEngine processEngine = engines.get(processEngineName);
                try {
                    processEngine.close();
                } catch (Exception e) {
                    LOGGER.error("exception while closing {}", (processEngineName == null ? "the default process engine" : "process engine " + processEngineName), e);
                }
            }

            processEngineInfosByName.clear();
            processEngineInfosByResourceUrl.clear();
            processEngineInfos.clear();

            setInitialized(false);
        }
    }

    public static boolean isInitialized() {
        return isInitialized;
    }

    public static void setInitialized(boolean isInitialized) {
        ProcessEngines.isInitialized = isInitialized;
    }
}
