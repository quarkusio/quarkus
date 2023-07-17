package io.quarkus.hibernate.orm.envers.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.persistence.metamodel.Bindable;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.envers.MyAuditedEntity;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusUnitTest;

public class ConfigEnabledFalseAndActiveTrueTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar.addClass(MyAuditedEntity.class))
            .withConfigurationResource("application.properties")
            .overrideConfigKey("quarkus.hibernate-envers.enabled", "false")
            .overrideConfigKey("quarkus.hibernate-envers.active", "true")
            .assertException(throwable -> assertThat(throwable)
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining(
                            "Hibernate Envers activated explicitly for persistence unit '<default>', but the Hibernate Envers extension was disabled at build time",
                            "If you want Hibernate Envers to be active for this persistence unit, you must set 'quarkus.hibernate-envers.enabled' to 'true' at build time",
                            "If you don't want Hibernate Envers to be active for this persistence unit, you must leave 'quarkus.hibernate-envers.active' unset or set it to 'false'"));

    @Inject
    SessionFactory sessionFactory;

    @Test
    public void test() {
        assertThat(sessionFactory.getMetamodel().getEntities())
                .extracting(Bindable::getBindableJavaType)
                // In particular this should not contain the revision entity
                .containsExactlyInAnyOrder((Class) MyAuditedEntity.class);

        try (Session session = sessionFactory.openSession()) {
            assertThatThrownBy(() -> AuditReaderFactory.get(session).isEntityClassAudited(MyAuditedEntity.class))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Service is not yet initialized");
        }
    }
}
