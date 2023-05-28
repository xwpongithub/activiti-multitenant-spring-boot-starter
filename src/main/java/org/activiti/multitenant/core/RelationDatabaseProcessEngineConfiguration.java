package org.activiti.multitenant.core;

import org.activiti.engine.impl.cfg.StandaloneProcessEngineConfiguration;

import javax.sql.DataSource;

public class RelationDatabaseProcessEngineConfiguration extends StandaloneProcessEngineConfiguration {

    public RelationDatabaseProcessEngineConfiguration(String databaseSchemaUpdate, DataSource dataSource) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
        this.dataSource = dataSource;
    }

    public RelationDatabaseProcessEngineConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

}
