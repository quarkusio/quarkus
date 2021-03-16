package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.internal.JdbcEnvironmentImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.reactive.provider.service.NoJdbcEnvironmentInitiator;
import org.hibernate.service.spi.ServiceRegistryImplementor;

public class QuarkusNoJdbcEnvironmentInitiator extends NoJdbcEnvironmentInitiator {

    private final Dialect dialect;

    public QuarkusNoJdbcEnvironmentInitiator(Dialect dialect) {
        this.dialect = dialect;
    }

    @Override
    public Class<JdbcEnvironment> getServiceInitiated() {
        return JdbcEnvironment.class;
    }

    @Override
    public JdbcEnvironment initiateService(Map configurationValues, ServiceRegistryImplementor registry) {
        return new JdbcEnvironmentImpl(registry, dialect);
    }

}
