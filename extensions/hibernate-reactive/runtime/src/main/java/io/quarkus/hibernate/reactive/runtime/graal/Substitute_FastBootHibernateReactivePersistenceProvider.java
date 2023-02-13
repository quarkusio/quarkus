package io.quarkus.hibernate.reactive.runtime.graal;

import java.util.Map;
import java.util.function.BooleanSupplier;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.spi.PersistenceUnitInfo;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.hibernate.orm.runtime.FastBootHibernatePersistenceProvider;
import io.quarkus.hibernate.reactive.runtime.graal.Substitute_FastBootHibernateReactivePersistenceProvider.IsAgroalAbsent;

@TargetClass(className = "io.quarkus.hibernate.reactive.runtime.FastBootHibernateReactivePersistenceProvider", onlyWith = IsAgroalAbsent.class)
public final class Substitute_FastBootHibernateReactivePersistenceProvider {

    @Substitute
    @SuppressWarnings("rawtypes")
    public EntityManagerFactory createContainerEntityManagerFactory(PersistenceUnitInfo info, Map map) {
        throw new IllegalStateException(
                "This method should not be used if we don't have a JDBC datasource to start a regular JDBC-based Hibernate ORM EntityManager.");
    }

    @Substitute
    @SuppressWarnings("rawtypes")
    public void generateSchema(PersistenceUnitInfo info, Map map) {
        throw new IllegalStateException(
                "This method should not be used if we don't have a JDBC datasource to start a regular JDBC-based Hibernate ORM EntityManager.");
    }

    @Substitute
    @SuppressWarnings("rawtypes")
    public boolean generateSchema(String persistenceUnitName, Map map) {
        throw new IllegalStateException(
                "This method should not be used if we don't have a JDBC datasource to start a regular JDBC-based Hibernate ORM EntityManager.");
    }

    @Substitute
    private FastBootHibernatePersistenceProvider getJdbcHibernatePersistenceProviderDelegate() {
        throw new IllegalStateException(
                "This method should not be used if we don't have a JDBC datasource to start a regular JDBC-based Hibernate ORM EntityManager.");
    }

    public static class IsAgroalAbsent implements BooleanSupplier {

        private boolean agroalAbsent;

        public IsAgroalAbsent() {
            try {
                Class.forName("io.quarkus.agroal.DataSource");
                agroalAbsent = false;
            } catch (ClassNotFoundException e) {
                agroalAbsent = true;
            }
        }

        @Override
        public boolean getAsBoolean() {
            return agroalAbsent;
        }
    }
}
