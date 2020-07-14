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
    public String environment;

    /**
     * MyBatis transaction factory
     */
    @ConfigItem(defaultValue = "MANAGED")
    public String transactionFactory;

    /**
     * MyBatis data source
     */
    @ConfigItem(name = "datasource")
    public Optional<String> dataSource;

    /**
     * MyBatis initial sql
     */
    @ConfigItem(name = "initial-sql")
    public Optional<String> initialSql;
}
