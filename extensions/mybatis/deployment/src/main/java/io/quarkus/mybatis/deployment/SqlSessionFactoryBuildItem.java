package io.quarkus.mybatis.deployment;

import org.apache.ibatis.session.SqlSessionFactory;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Hold the RuntimeValue of {@link SqlSessionFactory}
 */
public final class SqlSessionFactoryBuildItem extends SimpleBuildItem {
    private final RuntimeValue<SqlSessionFactory> sqlSessionFactory;

    public SqlSessionFactoryBuildItem(RuntimeValue<SqlSessionFactory> sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    public RuntimeValue<SqlSessionFactory> getSqlSessionFactory() {
        return sqlSessionFactory;
    }
}
