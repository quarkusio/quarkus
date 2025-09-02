package io.quarkus.hibernate.orm.formatmapper;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class FormatMapperBehaviorXmlFailsByDefaultTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyXmlEntity.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .assertException(ex -> assertThat(ex).hasCauseInstanceOf(IllegalStateException.class)
                    .cause()
                    .hasMessageContaining("set \"quarkus.hibernate-orm.mapping.format.global=ignore\""));

    @Inject
    SessionFactory sessionFactory;

    @Test
    void smoke() {
    }
}
