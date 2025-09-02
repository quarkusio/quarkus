package io.quarkus.hibernate.orm.formatmapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.SchemaUtil;
import io.quarkus.hibernate.orm.SmokeTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class FormatMapperBehaviorJsonObjectMapperCustomizerTest {
    @RegisterExtension
    static QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyJsonEntity.class, MyObjectMapperCustomizer.class)
                    .addClasses(SchemaUtil.class, SmokeTestUtils.class))
            .withConfigurationResource("application.properties")
            .assertException(ex -> assertThat(ex).hasCauseInstanceOf(IllegalStateException.class)
                    .cause()
                    .hasMessageContaining("set \"quarkus.hibernate-orm.mapping.format.global=ignore\""));

    @Test
    void smoke() {
    }
}
