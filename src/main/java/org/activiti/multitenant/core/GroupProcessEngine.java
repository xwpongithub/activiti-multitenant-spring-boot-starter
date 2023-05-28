package org.activiti.multitenant.core;

import lombok.Data;
import org.activiti.engine.ProcessEngine;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GroupProcessEngine {

    private String groupName;

//    private DynamicDataSourceStrategy dynamicDataSourceStrategy;

    private Map<String, ProcessEngine> processEngineMap = new ConcurrentHashMap<>();

    public GroupProcessEngine(String groupName) {

//    public GroupProcessEngine(String groupName, DynamicDataSourceStrategy dynamicDataSourceStrategy) {
        this.groupName = groupName;
//        this.dynamicDataSourceStrategy = dynamicDataSourceStrategy;
    }

    /**
     * add a new process engine to this group
     *
     * @param ds         the name of the datasource
     * @param processEngine processEngine
     */
    public ProcessEngine addProcessEngine(String ds, ProcessEngine processEngine) {
        return processEngineMap.put(ds, processEngine);
    }

    /**
     * @param ds the name of the datasource
     */
    public ProcessEngine removeProcessEngine(String ds) {
        return processEngineMap.remove(ds);
    }

    public String determineProcessEngineKey() {
//        return dynamicDataSourceStrategy.determineKey(new ArrayList<>(dataSourceMap.keySet()));
        return "master";
    }

    public ProcessEngine determineProcessEngine() {
        return processEngineMap.get(determineProcessEngineKey());
    }

    public int size() {
        return processEngineMap.size();
    }
}
