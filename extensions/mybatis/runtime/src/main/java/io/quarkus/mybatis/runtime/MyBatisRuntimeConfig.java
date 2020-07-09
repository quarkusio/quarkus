package io.quarkus.mybatis.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "mybatis")
public class MyBatisRuntimeConfig {

    /**
     * MyBatis environment id
     */
    @ConfigItem(defaultValue = "quarkus")
    public String environment = "quarkus";

    /**
     * MyBatis transaction factory
     */
    @ConfigItem(defaultValue = "MANAGED")
    public String transactionFactory = "MANAGED";

    /**
     * MyBatis data source
     */
    @ConfigItem(name = "datasource")
    public Optional<String> dataSource;

    /**
     * MyBatis initial sql
     */
    @ConfigItem(name = "initialSql")
    public Optional<String> initialSql;
}
