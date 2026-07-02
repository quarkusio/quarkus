package io.quarkus.hibernate.orm.panache.deployment.test.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.orm.panache.deployment.test.MyEntity;
import io.quarkus.maven.dependency.ArtifactKey;
import io.quarkus.test.QuarkusExtensionTest;

public class JdbcDriverMissingEntitiesTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class))
            .setExcludedDependencies(Set.of(
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2"),
                    ArtifactKey.of("io.quarkus", "quarkus-jdbc-h2-deployment")))
            .assertException(t -> assertThat(t)
                    .hasMessageContainingAll(
                            "Persistence unit '<default>' defines entities [" + MyEntity.class.getName()
                                    + "], but its datasource '<default>' cannot be found",
                            "Datasource '<default>' is not configured.",
                            "To solve this, configure datasource '<default>'",
                            "Refer to https://quarkus.io/guides/datasource for guidance.",
                            "Alternatively, disable Hibernate ORM by setting 'quarkus.hibernate-orm.enabled=false', and the entities will be ignored"));

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
