package io.quarkus.mybatis.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.apache.ibatis.session.SqlSessionFactory;

@Singleton
public class MyBatisProducers {
    private volatile SqlSessionFactory sqlSessionFactory;

    public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Singleton
    @Produces
    SqlSessionFactory sqlSessionFactory() {
        return this.sqlSessionFactory;
    }
}
