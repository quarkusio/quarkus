package io.quarkus.mybatis.runtime;

import java.io.Reader;
import java.sql.Connection;
import java.util.List;
import java.util.function.Supplier;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.session.SqlSessionManager;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.jboss.logging.Logger;

import io.quarkus.agroal.runtime.DataSources;
import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MyBatisRecorder {
    private static final Logger LOG = Logger.getLogger(MyBatisRecorder.class);

    public RuntimeValue<SqlSessionFactory> createSqlSessionFactory(
            String environment, String transactionFactory, String dataSourceName, List<String> mappers) {
        Configuration configuration = new Configuration();

        TransactionFactory factory;
        if (transactionFactory.equals("MANAGED")) {
            factory = new ManagedTransactionFactory();
        } else {
            factory = new JdbcTransactionFactory();
        }

        Environment.Builder environmentBuilder = new Environment.Builder(environment).transactionFactory(factory).dataSource(
                DataSources.fromName(dataSourceName));

        configuration.setEnvironment(environmentBuilder.build());
        for (String mapper : mappers) {
            try {
                configuration.addMapper(Resources.classForName(mapper));
            } catch (ClassNotFoundException e) {

            }
        }

        SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
        return new RuntimeValue<>(sqlSessionFactory);
    }

    public RuntimeValue<SqlSessionManager> createSqlSessionManager(RuntimeValue<SqlSessionFactory> sqlSessionFactory) {
        SqlSessionManager sqlSessionManager = SqlSessionManager.newInstance(sqlSessionFactory.getValue());
        return new RuntimeValue<>(sqlSessionManager);
    }

    public Supplier<Object> MyBatisMapperSupplier(String name, RuntimeValue<SqlSessionManager> sqlSessionManager) {
        return () -> {
            try {
                return sqlSessionManager.getValue().getMapper(Resources.classForName(name));
            } catch (ClassNotFoundException e) {
                return null;
            }
        };
    }

    public void runInitialSql(RuntimeValue<SqlSessionFactory> sqlSessionFactory, String sql) {
        try (SqlSession session = sqlSessionFactory.getValue().openSession()) {
            Connection conn = session.getConnection();
            Reader reader = Resources.getResourceAsReader(sql);
            ScriptRunner runner = new ScriptRunner(conn);
            runner.setLogWriter(null);
            runner.runScript(reader);
            reader.close();
        } catch (Exception e) {
            LOG.warn("Error executing SQL Script " + sql);
        }
    }

    public void register(RuntimeValue<SqlSessionFactory> sqlSessionFactory, BeanContainer beanContainer) {
        beanContainer.instance(MyBatisProducers.class).setSqlSessionFactory(sqlSessionFactory.getValue());
    }
}
