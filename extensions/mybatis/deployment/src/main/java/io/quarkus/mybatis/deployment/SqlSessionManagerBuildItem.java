package io.quarkus.mybatis.deployment;

import org.apache.ibatis.session.SqlSessionManager;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.RuntimeValue;

/**
 * Hold the RuntimeValue of {@link SqlSessionManager}
 */
public final class SqlSessionManagerBuildItem extends SimpleBuildItem {
    private final RuntimeValue<SqlSessionManager> sqlSessionManager;

    public SqlSessionManagerBuildItem(RuntimeValue<SqlSessionManager> sqlSessionManager) {
        this.sqlSessionManager = sqlSessionManager;
    }

    public RuntimeValue<SqlSessionManager> getSqlSessionManager() {
        return sqlSessionManager;
    }
}
