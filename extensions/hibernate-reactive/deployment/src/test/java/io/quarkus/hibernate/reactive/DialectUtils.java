package io.quarkus.hibernate.reactive;

import java.lang.reflect.InvocationTargetException;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.reactive.mutiny.Mutiny;
import org.hibernate.reactive.mutiny.impl.MutinySessionFactoryImpl;
import org.hibernate.service.ServiceRegistry;

import io.quarkus.arc.ClientProxy;
import io.quarkus.hibernate.orm.runtime.config.DialectVersions;

public class DialectUtils {

    private DialectUtils() {
    }

    public static String getDefaultVersion(Class<? extends Dialect> dialectClass) {
        try {
            return DialectVersions.toString(dialectClass.getConstructor().newInstance().getVersion());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getConfiguredVersion(Mutiny.SessionFactory sessionFactory) {
        ServiceRegistry serviceRegistry = ((MutinySessionFactoryImpl) ClientProxy.unwrap(sessionFactory))
                .getServiceRegistry();
        return DialectVersions.toString(serviceRegistry.requireService(JdbcEnvironment.class).getDialect().getVersion());
    }
}
