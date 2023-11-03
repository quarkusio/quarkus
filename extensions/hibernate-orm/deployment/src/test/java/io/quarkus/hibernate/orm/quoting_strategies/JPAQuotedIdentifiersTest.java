package io.quarkus.hibernate.orm.quoting_strategies;

import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;

/**
 * Failed to fetch entity with reserved name and containing one column with reserved name and column definition.
 * <p>
 * To resolve the simulated situation, this test uses the quoting strategy {@code all-except-column-definitions}.
 */
public class JPAQuotedIdentifiersTest extends AbstractJPAQuotedTest {

    @RegisterExtension
    final static QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(Group.class, QuotedResource.class)
                    .addAsResource("application-quoted-identifiers.properties", "application.properties"));

}
