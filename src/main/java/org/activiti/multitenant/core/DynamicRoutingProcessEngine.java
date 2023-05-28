package org.activiti.multitenant.core;

import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DynamicRoutingProcessEngine  implements InitializingBean, DisposableBean {

    private static final String UNDERLINE = "_";
    /**
     * 所有流程引擎
     */
    private final Map<String, ProcessEngine> processEngineMap = new ConcurrentHashMap<>();
    /**
     * 分组流程引擎
     */
    private final Map<String, GroupProcessEngine> groupProcessEngines = new ConcurrentHashMap<>();

    @Autowired
    private List<DynamicDataSourceProvider> providers;

//    @Setter
//    private Class<? extends DynamicDataSourceStrategy> strategy = LoadBalanceDynamicDataSourceStrategy.class;
    @Setter
    private String primary = "master";
    @Setter
    private Boolean strict = false;

    protected String getPrimary() {
        return primary;
    }

    public ProcessEngine determineProcessEngine() {
        String dsKey = DynamicProcessEngineContextHolder.peek();
        return getProcessEngine(dsKey);
    }


    private ProcessEngine determinePrimaryProcessEngine() {
        log.debug("process engine switch to the primary process engine");
        ProcessEngine processEngine = processEngineMap.get(primary);
        if (Objects.nonNull(processEngine)) {
            return processEngine;
        }
        GroupProcessEngine groupProcessEngine = groupProcessEngines.get(primary);
        if (Objects.nonNull(groupProcessEngine)) {
            return groupProcessEngine.determineProcessEngine();
        }
        throw new CannotFindProcessEngineException("dynamic-process-engine can not find primary process engine");
    }

    /**
     * 获取所有的流程引擎
     *
     * @return 当前所有流程引擎
     */
    public Map<String, ProcessEngine> getProcessEngines() {
        return processEngineMap;
    }

    /**
     * 获取的所有的分组流程引擎
     *
     * @return 当前所有的分组流程引擎
     */
    public Map<String, GroupProcessEngine> getGroupProcessEngines() {
        return groupProcessEngines;
    }

    /**
     * 获取流程引擎
     *
     * @param ds 流程引擎名称
     * @return 流程引擎
     */
    public ProcessEngine getProcessEngine(String ds) {
        if (StringUtils.isBlank(ds)) {
            return determinePrimaryProcessEngine();
        } else if (!groupProcessEngines.isEmpty() && groupProcessEngines.containsKey(ds)) {
            log.debug("dynamic-process-engine switch to the process engine named [{}]", ds);
            return groupProcessEngines.get(ds).determineProcessEngine();
        } else if (processEngineMap.containsKey(ds)) {
            log.debug("dynamic-process-engine switch to the process engine named [{}]", ds);
            return processEngineMap.get(ds);
        }
        if (strict) {
            throw new CannotFindProcessEngineException("dynamic-process-engine could not find a process engine named" + ds);
        }
        return determinePrimaryProcessEngine();
    }

    /**
     * 添加流程引擎
     *
     * @param ds         流程引擎名称
     * @param processEngine 流程引擎
     */
    public synchronized void addProcessEngine(String ds, ProcessEngine processEngine) {
        ProcessEngine oldProcessEngine = processEngineMap.put(ds, processEngine);
        // 新数据源添加到分组
        this.addGroupProcessEngine(ds, processEngine);
        // 关闭老的数据源
        if (Objects.nonNull(oldProcessEngine)) {
            closeProcessEngine(ds, oldProcessEngine);
        }
        log.info("dynamic-process-engine - add a process engine named [{}] success", ds);
    }

    /**
     * 新流程引擎添加到分组
     *
     * @param ds         新流程引擎的名字
     * @param processEngine 新流程引擎
     */
    private void addGroupProcessEngine(String ds, ProcessEngine processEngine) {
        if (ds.contains(UNDERLINE)) {
            String group = ds.split(UNDERLINE)[0];
            GroupProcessEngine groupProcessEngine = groupProcessEngines.get(group);
            if (Objects.isNull(groupProcessEngine)) {
                try {
//                    groupProcessEngine = new GroupProcessEngine(group, strategy.getDeclaredConstructor().newInstance());
                    groupProcessEngine = new GroupProcessEngine(group);
                    groupProcessEngines.put(group, groupProcessEngine);
                } catch (Exception e) {
                    throw new RuntimeException("dynamic-process-engine - add the process engine named " + ds + " error", e);
                }
            }
            groupProcessEngine.addProcessEngine(ds, processEngine);
        }
    }

    /**
     * 删除流程引擎
     *
     * @param ds 流程引擎名称
     */
    public synchronized void removeDataSource(String ds) {
        if (StringUtils.isBlank(ds)) {
            throw new RuntimeException("remove parameter could not be empty");
        }
        if (primary.equals(ds)) {
            throw new RuntimeException("could not remove primary process engine");
        }
        if (processEngineMap.containsKey(ds)) {
            ProcessEngine processEngine = processEngineMap.remove(ds);
            closeProcessEngine(ds, processEngine);
            if (ds.contains(UNDERLINE)) {
                String group = ds.split(UNDERLINE)[0];
                if (groupProcessEngines.containsKey(group)) {
                    ProcessEngine oldProcessEngine = groupProcessEngines.get(group).removeProcessEngine(ds);
                    if (Objects.isNull(oldProcessEngine)) {
                        log.warn("fail for remove process engine from group. processEngine: {} ,group: {}", ds, group);
                    }
                }
            }
            log.info("dynamic-process-engine - remove the process engine named [{}] success", ds);
        } else {
            log.warn("dynamic-process-engine - could not find a process engine named [{}]", ds);
        }
    }

    @Override
    public void destroy() throws Exception {
        log.info("dynamic-process-engine start closing ....");
        for (Map.Entry<String, ProcessEngine> item : processEngineMap.entrySet()) {
            closeProcessEngine(item.getKey(), item.getValue());
        }
        log.info("dynamic-process-engine all closed success,bye");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // 检查开启了配置但没有相关依赖
        checkEnv();
        // 添加并分组流程引擎
        Map<String, ProcessEngine> processEngines = new HashMap<>(16);
        for (DynamicDataSourceProvider provider : providers) {
            Map<String,ProcessEngine> initializedProcessEngineMap = initProcessEngineMap(provider);
            processEngines.putAll(initializedProcessEngineMap);
        }
        for (Map.Entry<String, ProcessEngine> dsItem : processEngines.entrySet()) {
            addProcessEngine(dsItem.getKey(), dsItem.getValue());
        }
        // 检测默认数据源是否设置
        if (groupProcessEngines.containsKey(primary)) {
            log.info("dynamic-process-engine initial loaded [{}] process engine,primary group process engine named [{}]", processEngines.size(), primary);
        } else if (processEngineMap.containsKey(primary)) {
            log.info("dynamic-process-engine initial loaded [{}] process engine,primary process engine named [{}]", processEngines.size(), primary);
        } else {
            log.warn("dynamic-process-engine initial loaded [{}] process engine,Please add your primary process engine or check your configuration", processEngines.size());
        }
    }

    private Map<String, ProcessEngine> initProcessEngineMap(DynamicDataSourceProvider provider) {
        Map<String, DataSource> dsMap = provider.loadDataSources();
        Map<String,ProcessEngine> processEngineMap1 = new HashMap<>(16);
        dsMap.forEach((key,dataSource)-> {
            RelationDatabaseProcessEngineConfiguration processEngineConfiguration =
                    new RelationDatabaseProcessEngineConfiguration(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE,
                            dataSource);
            ProcessEngine processEngine = processEngineConfiguration.buildProcessEngine();
            processEngineMap1.put(key,processEngine);
        });
        return processEngineMap1;
    }


    private void checkEnv() {
//        if (p6spy) {
//            try {
//                Class.forName("com.p6spy.engine.spy.P6DataSource");
//                log.info("dynamic-datasource detect P6SPY plugin and enabled it");
//            } catch (Exception e) {
//                throw new RuntimeException("dynamic-datasource enabled P6SPY ,however without p6spy dependency", e);
//            }
//        }
//        if (seata) {
//            try {
//                Class.forName("io.seata.rm.datasource.DataSourceProxy");
//                log.info("dynamic-datasource detect ALIBABA SEATA and enabled it");
//            } catch (Exception e) {
//                throw new RuntimeException("dynamic-datasource enabled ALIBABA SEATA,however without seata dependency", e);
//            }
//        }
    }

    /**
     * close db
     *
     * @param ds         dsName
     * @param processEngine pe
     */
    private void closeProcessEngine(String ds, ProcessEngine processEngine) {
        try {
            Method closeMethod = ReflectionUtils.findMethod(processEngine.getClass(), "close");
            if (closeMethod != null) {
                closeMethod.invoke(processEngine);
            }
        } catch (Exception e) {
            log.warn("dynamic-process-engine closed process engine named [{}] failed", ds, e);
        }
    }

}
