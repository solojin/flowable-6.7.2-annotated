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

import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.common.engine.api.engine.EngineLifecycleListener;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.cfg.HttpClientConfig;
import org.flowable.engine.impl.cfg.StandaloneInMemProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.task.service.TaskPostProcessor;

/**
 * 流程引擎配置类
 *
 * 用于构建流程引擎的配置信息
 *
 * 最常见的是基于默认配置文件创建流程引擎：
 *
 * ProcessEngine processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault().buildProcessEngine();
 *
 * 要在没有配置文件的情况下通过编程创建流程引擎，第一个选项是{@link#createStandaloneProcessEngineConfiguration（）}
 * ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneProcessEngineConfiguration().buildProcessEngine();
 *
 * 在独立模式下，将创建一个包含连接到远程h2数据库的所有默认设置（jdbc:h2:tcp://localhost/flowable)的新流程引擎。独立模式意味着流程引擎将管理它创建的JDBC连接上的事务。每个服务方法一个事务。有关如何编写配置文件的说明，请参阅《用户指南》。
 *
 * 第二个选项非常适合测试：{@link#createStandaloneInMemProcessEngineConfiguration（）}
 *
 * ProcessEngine processEngine = ProcessEngineConfiguration.createStandaloneInMemProcessEngineConfiguration().buildProcessEngine();
 *
 * 在创建流程引擎的所有形式中，在调用{@link#buildProcessEngine（）}方法之前，可以先自定义配置，方法如下：
 * ProcessEngine processEngine = ProcessEngineConfiguration.createProcessEngineConfigurationFromResourceDefault().setMailServerHost(&quot;gmail.com&quot;).setJdbcUsername(&quot;mickey&quot;).setJdbcPassword(&quot;mouse&quot;)
 *           .buildProcessEngine();
 * 
 * @see ProcessEngines
 * @author Tom Baeyens
 */
public abstract class ProcessEngineConfiguration extends AbstractEngineConfiguration {

    protected String processEngineName = ProcessEngines.NAME_DEFAULT;
    protected int idBlockSize = 2500;
    protected String history = HistoryLevel.AUDIT.getKey();
    protected boolean asyncExecutorActivate;
    protected boolean asyncHistoryExecutorActivate;

    protected String mailServerHost = "localhost";
    protected String mailServerUsername; // 默认情况下，不提供任何名称和密码
    protected String mailServerPassword; // 意味着邮件服务器没有身份验证
    protected int mailServerPort = 25;
    protected int mailServerSSLPort = 465;
    protected boolean useSSL;
    protected boolean useTLS;
    protected String mailServerDefaultFrom = "flowable@localhost";
    protected String mailServerForceTo;
    protected Charset mailServerDefaultCharset;
    protected String mailSessionJndi;
    protected Map<String, MailServerInfo> mailServers = new HashMap<>();
    protected Map<String, String> mailSessionsJndi = new HashMap<>();

    // 设置Http客户端配置默认值
    protected HttpClientConfig httpClientConfig = new HttpClientConfig();

    protected HistoryLevel historyLevel;
    protected boolean enableProcessDefinitionHistoryLevel;

    protected String jpaPersistenceUnitName;
    protected Object jpaEntityManagerFactory;
    protected boolean jpaHandleTransaction;
    protected boolean jpaCloseEntityManager;

    protected AsyncExecutor asyncExecutor;
    protected AsyncTaskExecutor asyncTaskExecutor;
    protected boolean shutdownAsyncTaskExecutor;
    protected AsyncTaskInvoker asyncTaskInvoker;

    protected AsyncExecutor asyncHistoryExecutor;
    protected AsyncTaskExecutor asyncHistoryTaskExecutor;
    protected boolean shutdownAsyncHistoryTaskExecutor;

    /** 定义失败作业的默认等待时间（秒） */
    protected int defaultFailedJobWaitTime = 10;
    /** 定义失败异步作业的默认等待时间（秒） */
    protected int asyncFailedJobWaitTime = 10;

    /**
     * 流程图生成器。默认值为DefaultProcessDiagramGenerator
     */
    protected ProcessDiagramGenerator processDiagramGenerator;

    protected boolean isCreateDiagramOnDeploy = true;

    protected boolean alwaysUseArraysForDmnMultiHitPolicies = true;
    
    /**
     *  如果没有标签DI，请包含顺序流名称,
     */
    protected boolean drawSequenceFlowNameWithNoLabelDI = false;
    
    protected String defaultCamelContext = "camelContext";

    protected String activityFontName = "Arial";
    protected String labelFontName = "Arial";
    protected String annotationFontName = "Arial";

    protected boolean enableProcessDefinitionInfoCache;

    // 清除历史
    protected boolean enableHistoryCleaning = false;
    protected String historyCleaningTimeCycleConfig = "0 0 1 * * ?";
    protected Duration cleanInstancesEndedAfter = Duration.ofDays(365);
    protected int cleanInstancesBatchSize = 100;
    protected boolean cleanInstancesSequentially = false;
    protected HistoryCleaningManager historyCleaningManager;


    /** 任务生成器的后处理器 */
    protected TaskPostProcessor taskPostProcessor = null;

    /** 使用一个静态创建方法static createXxxx替代 */
    protected ProcessEngineConfiguration() {
    }

    public abstract ProcessEngine buildProcessEngine();

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResourceDefault() {
        return createProcessEngineConfigurationFromResource("flowable.cfg.xml", "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource) {
        return createProcessEngineConfigurationFromResource(resource, "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromResource(String resource, String beanName) {
        return (ProcessEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream) {
        return createProcessEngineConfigurationFromInputStream(inputStream, "processEngineConfiguration");
    }

    public static ProcessEngineConfiguration createProcessEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (ProcessEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static ProcessEngineConfiguration createStandaloneProcessEngineConfiguration() {
        return new StandaloneProcessEngineConfiguration();
    }

    public static ProcessEngineConfiguration createStandaloneInMemProcessEngineConfiguration() {
        return new StandaloneInMemProcessEngineConfiguration();
    }

    // 待我们对此进行测试覆盖后再添加
    // public static ProcessEngineConfiguration
    // createJtaProcessEngineConfiguration() {
    //      return new JtaProcessEngineConfiguration();
    // }

    public abstract RepositoryService getRepositoryService();

    public abstract RuntimeService getRuntimeService();

    public abstract FormService getFormService();

    public abstract TaskService getTaskService();

    public abstract HistoryService getHistoryService();

    public abstract IdentityService getIdentityService();

    public abstract ManagementService getManagementService();

    public abstract ProcessEngineConfiguration getProcessEngineConfiguration();

    // getters and setters 方法区
    // //////////////////////////////////////////////////////

    @Override
    public String getEngineName() {
        return processEngineName;
    }

    public ProcessEngineConfiguration setEngineName(String processEngineName) {
        this.processEngineName = processEngineName;
        return this;
    }

    public int getIdBlockSize() {
        return idBlockSize;
    }

    public ProcessEngineConfiguration setIdBlockSize(int idBlockSize) {
        this.idBlockSize = idBlockSize;
        return this;
    }

    public String getHistory() {
        return history;
    }

    public ProcessEngineConfiguration setHistory(String history) {
        this.history = history;
        return this;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    public ProcessEngineConfiguration setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
        return this;
    }

    public String getMailServerUsername() {
        return mailServerUsername;
    }

    public ProcessEngineConfiguration setMailServerUsername(String mailServerUsername) {
        this.mailServerUsername = mailServerUsername;
        return this;
    }

    public String getMailServerPassword() {
        return mailServerPassword;
    }

    public ProcessEngineConfiguration setMailServerPassword(String mailServerPassword) {
        this.mailServerPassword = mailServerPassword;
        return this;
    }

    public String getMailSessionJndi() {
        return mailSessionJndi;
    }

    public ProcessEngineConfiguration setMailSessionJndi(String mailSessionJndi) {
        this.mailSessionJndi = mailSessionJndi;
        return this;
    }

    public int getMailServerPort() {
        return mailServerPort;
    }

    public ProcessEngineConfiguration setMailServerPort(int mailServerPort) {
        this.mailServerPort = mailServerPort;
        return this;
    }

    public Charset getMailServerDefaultCharset() {
        return mailServerDefaultCharset;
    }

    public ProcessEngineConfiguration setMailServerDefaultCharset(Charset mailServerDefaultCharset) {
        this.mailServerDefaultCharset = mailServerDefaultCharset;
        return this;
    }

    public int getMailServerSSLPort() {
        return mailServerSSLPort;
    }

    public ProcessEngineConfiguration setMailServerSSLPort(int mailServerSSLPort) {
        this.mailServerSSLPort = mailServerSSLPort;
        return this;
    }

    public boolean getMailServerUseSSL() {
        return useSSL;
    }

    public ProcessEngineConfiguration setMailServerUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public boolean getMailServerUseTLS() {
        return useTLS;
    }

    public ProcessEngineConfiguration setMailServerUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
        return this;
    }

    public String getMailServerDefaultFrom() {
        return mailServerDefaultFrom;
    }

    public ProcessEngineConfiguration setMailServerDefaultFrom(String mailServerDefaultFrom) {
        this.mailServerDefaultFrom = mailServerDefaultFrom;
        return this;
    }

    public String getMailServerForceTo() {
        return mailServerForceTo;
    }

    public ProcessEngineConfiguration setMailServerForceTo(String mailServerForceTo) {
        this.mailServerForceTo = mailServerForceTo;
        return this;
    }

    public MailServerInfo getMailServer(String tenantId) {
        return mailServers.get(tenantId);
    }

    public Map<String, MailServerInfo> getMailServers() {
        return mailServers;
    }

    public ProcessEngineConfiguration setMailServers(Map<String, MailServerInfo> mailServers) {
        this.mailServers.putAll(mailServers);
        return this;
    }

    public String getMailSessionJndi(String tenantId) {
        return mailSessionsJndi.get(tenantId);
    }

    public Map<String, String> getMailSessionsJndi() {
        return mailSessionsJndi;
    }

    public ProcessEngineConfiguration setMailSessionsJndi(Map<String, String> mailSessionsJndi) {
        this.mailSessionsJndi.putAll(mailSessionsJndi);
        return this;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public void setHttpClientConfig(HttpClientConfig httpClientConfig) {
        this.httpClientConfig.merge(httpClientConfig);
    }

    @Override
    public ProcessEngineConfiguration setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setTransactionsExternallyManaged(boolean transactionsExternallyManaged) {
        this.transactionsExternallyManaged = transactionsExternallyManaged;
        return this;
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public ProcessEngineConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }

    public boolean isEnableProcessDefinitionHistoryLevel() {
        return enableProcessDefinitionHistoryLevel;
    }

    public ProcessEngineConfiguration setEnableProcessDefinitionHistoryLevel(boolean enableProcessDefinitionHistoryLevel) {
        this.enableProcessDefinitionHistoryLevel = enableProcessDefinitionHistoryLevel;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxActiveConnections(int jdbcMaxActiveConnections) {
        this.jdbcMaxActiveConnections = jdbcMaxActiveConnections;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxIdleConnections(int jdbcMaxIdleConnections) {
        this.jdbcMaxIdleConnections = jdbcMaxIdleConnections;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxCheckoutTime(int jdbcMaxCheckoutTime) {
        this.jdbcMaxCheckoutTime = jdbcMaxCheckoutTime;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcMaxWaitTime(int jdbcMaxWaitTime) {
        this.jdbcMaxWaitTime = jdbcMaxWaitTime;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPingEnabled(boolean jdbcPingEnabled) {
        this.jdbcPingEnabled = jdbcPingEnabled;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPingQuery(String jdbcPingQuery) {
        this.jdbcPingQuery = jdbcPingQuery;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcPingConnectionNotUsedFor(int jdbcPingNotUsedFor) {
        this.jdbcPingConnectionNotUsedFor = jdbcPingNotUsedFor;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setJdbcDefaultTransactionIsolationLevel(int jdbcDefaultTransactionIsolationLevel) {
        this.jdbcDefaultTransactionIsolationLevel = jdbcDefaultTransactionIsolationLevel;
        return this;
    }

    public boolean isAsyncExecutorActivate() {
        return asyncExecutorActivate;
    }

    public ProcessEngineConfiguration setAsyncExecutorActivate(boolean asyncExecutorActivate) {
        this.asyncExecutorActivate = asyncExecutorActivate;
        return this;
    }
    
    public boolean isAsyncHistoryExecutorActivate() {
        return asyncHistoryExecutorActivate;
    }

    public ProcessEngineConfiguration setAsyncHistoryExecutorActivate(boolean asyncHistoryExecutorActivate) {
        this.asyncHistoryExecutorActivate = asyncHistoryExecutorActivate;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setUseClassForNameClassLoading(boolean useClassForNameClassLoading) {
        this.useClassForNameClassLoading = useClassForNameClassLoading;
        return this;
    }

    public Object getJpaEntityManagerFactory() {
        return jpaEntityManagerFactory;
    }

    public ProcessEngineConfiguration setJpaEntityManagerFactory(Object jpaEntityManagerFactory) {
        this.jpaEntityManagerFactory = jpaEntityManagerFactory;
        return this;
    }

    public boolean isJpaHandleTransaction() {
        return jpaHandleTransaction;
    }

    public ProcessEngineConfiguration setJpaHandleTransaction(boolean jpaHandleTransaction) {
        this.jpaHandleTransaction = jpaHandleTransaction;
        return this;
    }

    public boolean isJpaCloseEntityManager() {
        return jpaCloseEntityManager;
    }

    public ProcessEngineConfiguration setJpaCloseEntityManager(boolean jpaCloseEntityManager) {
        this.jpaCloseEntityManager = jpaCloseEntityManager;
        return this;
    }

    public String getJpaPersistenceUnitName() {
        return jpaPersistenceUnitName;
    }

    public ProcessEngineConfiguration setJpaPersistenceUnitName(String jpaPersistenceUnitName) {
        this.jpaPersistenceUnitName = jpaPersistenceUnitName;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDataSourceJndiName(String dataSourceJndiName) {
        this.dataSourceJndiName = dataSourceJndiName;
        return this;
    }

    public String getDefaultCamelContext() {
        return defaultCamelContext;
    }

    public ProcessEngineConfiguration setDefaultCamelContext(String defaultCamelContext) {
        this.defaultCamelContext = defaultCamelContext;
        return this;
    }

    public boolean isCreateDiagramOnDeploy() {
        return isCreateDiagramOnDeploy;
    }

    public ProcessEngineConfiguration setCreateDiagramOnDeploy(boolean createDiagramOnDeploy) {
        this.isCreateDiagramOnDeploy = createDiagramOnDeploy;
        return this;
    }
    
    public boolean isDrawSequenceFlowNameWithNoLabelDI() {
        return drawSequenceFlowNameWithNoLabelDI;
    }
    
    public ProcessEngineConfiguration setDrawSequenceFlowNameWithNoLabelDI(boolean drawSequenceFlowNameWithNoLabelDI) {
        this.drawSequenceFlowNameWithNoLabelDI = drawSequenceFlowNameWithNoLabelDI;
        return this;
    }
    
    public String getActivityFontName() {
        return activityFontName;
    }

    public ProcessEngineConfiguration setActivityFontName(String activityFontName) {
        this.activityFontName = activityFontName;
        return this;
    }

    /**
     * @deprecated 废弃方法 使用 {@link #setEngineLifecycleListeners(List)} 替代
     */
    @Deprecated
    public ProcessEngineConfiguration setProcessEngineLifecycleListener(ProcessEngineLifecycleListener processEngineLifecycleListener) {
        // 向后兼容性（当只有一个类型化引擎监听器时）
        if (engineLifecycleListeners == null || engineLifecycleListeners.isEmpty()) {
            List<EngineLifecycleListener> engineLifecycleListeners = new ArrayList<>(1);
            engineLifecycleListeners.add(processEngineLifecycleListener);
            super.setEngineLifecycleListeners(engineLifecycleListeners);

        } else {
            ProcessEngineLifecycleListener originalEngineLifecycleListener = (ProcessEngineLifecycleListener) engineLifecycleListeners.get(0);

            ProcessEngineLifecycleListener wrappingEngineLifecycleListener = new ProcessEngineLifecycleListener() {

                @Override
                public void onProcessEngineBuilt(ProcessEngine processEngine) {
                    originalEngineLifecycleListener.onProcessEngineBuilt(processEngine);
                }
                @Override
                public void onProcessEngineClosed(ProcessEngine processEngine) {
                    originalEngineLifecycleListener.onProcessEngineClosed(processEngine);
                }
            };

            engineLifecycleListeners.set(0, wrappingEngineLifecycleListener);

        }

        return this;
    }

    /**
     * @deprecated 废弃方法 使用 {@link #getEngineLifecycleListeners()} 替代
     */
    @Deprecated
    public ProcessEngineLifecycleListener getProcessEngineLifecycleListener() {
        // Backwards compatibility (when there was only one typed engine listener)
        if (engineLifecycleListeners != null && !engineLifecycleListeners.isEmpty()) {
            return (ProcessEngineLifecycleListener) engineLifecycleListeners.get(0);
        }
        return null;
    }

    public String getLabelFontName() {
        return labelFontName;
    }

    public ProcessEngineConfiguration setLabelFontName(String labelFontName) {
        this.labelFontName = labelFontName;
        return this;
    }

    public String getAnnotationFontName() {
        return annotationFontName;
    }

    public ProcessEngineConfiguration setAnnotationFontName(String annotationFontName) {
        this.annotationFontName = annotationFontName;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseTablePrefix(String databaseTablePrefix) {
        this.databaseTablePrefix = databaseTablePrefix;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setTablePrefixIsSchema(boolean tablePrefixIsSchema) {
        this.tablePrefixIsSchema = tablePrefixIsSchema;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseWildcardEscapeCharacter(String databaseWildcardEscapeCharacter) {
        this.databaseWildcardEscapeCharacter = databaseWildcardEscapeCharacter;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseCatalog(String databaseCatalog) {
        this.databaseCatalog = databaseCatalog;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setDatabaseSchema(String databaseSchema) {
        this.databaseSchema = databaseSchema;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setXmlEncoding(String xmlEncoding) {
        this.xmlEncoding = xmlEncoding;
        return this;
    }

    @Override
    public ProcessEngineConfiguration setClock(Clock clock) {
        this.clock = clock;
        return this;
    }

    public ProcessDiagramGenerator getProcessDiagramGenerator() {
        return this.processDiagramGenerator;
    }

    public ProcessEngineConfiguration setProcessDiagramGenerator(ProcessDiagramGenerator processDiagramGenerator) {
        this.processDiagramGenerator = processDiagramGenerator;
        return this;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public ProcessEngineConfiguration setAsyncExecutor(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        return this;
    }
    
    public AsyncTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }

    public ProcessEngineConfiguration setAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        return this;
    }

    public AsyncTaskInvoker getAsyncTaskInvoker() {
        return asyncTaskInvoker;
    }

    public ProcessEngineConfiguration setAsyncTaskInvoker(AsyncTaskInvoker asyncTaskInvoker) {
        this.asyncTaskInvoker = asyncTaskInvoker;
        return this;
    }

    public AsyncExecutor getAsyncHistoryExecutor() {
        return asyncHistoryExecutor;
    }

    public ProcessEngineConfiguration setAsyncHistoryExecutor(AsyncExecutor asyncHistoryExecutor) {
        this.asyncHistoryExecutor = asyncHistoryExecutor;
        return this;
    }

    public AsyncTaskExecutor getAsyncHistoryTaskExecutor() {
        return asyncHistoryTaskExecutor;
    }

    public ProcessEngineConfiguration setAsyncHistoryTaskExecutor(AsyncTaskExecutor asyncHistoryTaskExecutor) {
        this.asyncHistoryTaskExecutor = asyncHistoryTaskExecutor;
        return this;
    }

    public int getDefaultFailedJobWaitTime() {
        return defaultFailedJobWaitTime;
    }

    public ProcessEngineConfiguration setDefaultFailedJobWaitTime(int defaultFailedJobWaitTime) {
        this.defaultFailedJobWaitTime = defaultFailedJobWaitTime;
        return this;
    }

    public int getAsyncFailedJobWaitTime() {
        return asyncFailedJobWaitTime;
    }

    public ProcessEngineConfiguration setAsyncFailedJobWaitTime(int asyncFailedJobWaitTime) {
        this.asyncFailedJobWaitTime = asyncFailedJobWaitTime;
        return this;
    }

    public boolean isEnableProcessDefinitionInfoCache() {
        return enableProcessDefinitionInfoCache;
    }

    public ProcessEngineConfiguration setEnableProcessDefinitionInfoCache(boolean enableProcessDefinitionInfoCache) {
        this.enableProcessDefinitionInfoCache = enableProcessDefinitionInfoCache;
        return this;
    }

    public TaskPostProcessor getTaskPostProcessor() {
        return taskPostProcessor;
    }

    public void setTaskPostProcessor(TaskPostProcessor processor) {
        this.taskPostProcessor = processor;
    }


    public boolean isEnableHistoryCleaning() {
        return enableHistoryCleaning;
    }

    public ProcessEngineConfiguration setEnableHistoryCleaning(boolean enableHistoryCleaning) {
        this.enableHistoryCleaning = enableHistoryCleaning;
        return this;
    }

    public String getHistoryCleaningTimeCycleConfig() {
        return historyCleaningTimeCycleConfig;
    }

    public ProcessEngineConfiguration setHistoryCleaningTimeCycleConfig(String historyCleaningTimeCycleConfig) {
        this.historyCleaningTimeCycleConfig = historyCleaningTimeCycleConfig;
        return this;
    }

    /**
     * @deprecated 废弃方法 使用 {@link #getCleanInstancesEndedAfter()} 替代
     */
    @Deprecated
    public int getCleanInstancesEndedAfterNumberOfDays() {
        return (int) cleanInstancesEndedAfter.toDays();
    }

    /**
     * @deprecated 使用 {@link #setCleanInstancesEndedAfter(Duration)} 替代
     */
    @Deprecated
    public ProcessEngineConfiguration setCleanInstancesEndedAfterNumberOfDays(int cleanInstancesEndedAfterNumberOfDays) {
        return setCleanInstancesEndedAfter(Duration.ofDays(cleanInstancesEndedAfterNumberOfDays));
    }

    public Duration getCleanInstancesEndedAfter() {
        return cleanInstancesEndedAfter;
    }

    public ProcessEngineConfiguration setCleanInstancesEndedAfter(Duration cleanInstancesEndedAfter) {
        this.cleanInstancesEndedAfter = cleanInstancesEndedAfter;
        return this;
    }

    public int getCleanInstancesBatchSize() {
        return cleanInstancesBatchSize;
    }

    public ProcessEngineConfiguration setCleanInstancesBatchSize(int cleanInstancesBatchSize) {
        this.cleanInstancesBatchSize = cleanInstancesBatchSize;
        return this;
    }

    public boolean isCleanInstancesSequentially() {
        return cleanInstancesSequentially;
    }

    public ProcessEngineConfiguration setCleanInstancesSequentially(boolean cleanInstancesSequentially) {
        this.cleanInstancesSequentially = cleanInstancesSequentially;
        return this;
    }

    public HistoryCleaningManager getHistoryCleaningManager() {
        return historyCleaningManager;
    }

    public ProcessEngineConfiguration setHistoryCleaningManager(HistoryCleaningManager historyCleaningManager) {
        this.historyCleaningManager = historyCleaningManager;
        return this;
    }

    public boolean isAlwaysUseArraysForDmnMultiHitPolicies() {
        return alwaysUseArraysForDmnMultiHitPolicies;
    }

    public ProcessEngineConfiguration setAlwaysUseArraysForDmnMultiHitPolicies(boolean alwaysUseArraysForDmnMultiHitPolicies) {
        this.alwaysUseArraysForDmnMultiHitPolicies = alwaysUseArraysForDmnMultiHitPolicies;
        return this;
    }
}
