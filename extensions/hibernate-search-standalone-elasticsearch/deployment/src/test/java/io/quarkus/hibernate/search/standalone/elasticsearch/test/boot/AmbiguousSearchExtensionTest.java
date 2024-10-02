package io.quarkus.hibernate.search.standalone.elasticsearch.test.boot;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.SearchEntity;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.search.standalone.elasticsearch.SearchExtension;
import io.quarkus.test.QuarkusUnitTest;

public class AmbiguousSearchExtensionTest {
    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addClass(SearchFailureHandler.class)
                    .addClass(AnotherSearchFailureHandler.class)
                    .addAsResource("application.properties"))
            .assertException(throwable -> assertThat(throwable)
                    .hasNoSuppressedExceptions()
                    .rootCause()
                    .hasMessageContainingAll(
                            "Multiple instances of FailureHandler were found for Hibernate Search Standalone.",
                            "At most one instance can be assigned. Instances found:",
                            "io.quarkus.hibernate.search.standalone.elasticsearch.test.boot.AmbiguousSearchExtensionTest.SearchFailureHandler",
                            "io.quarkus.hibernate.search.standalone.elasticsearch.test.boot.AmbiguousSearchExtensionTest.AnotherSearchFailureHandler"));

    @SearchEntity
    @Indexed
    public static class MyEntity {
        @DocumentId
        private Long id;
    }

    @SearchExtension
    public static class SearchFailureHandler implements FailureHandler {
        @Override
        public void handle(FailureContext failureContext) {
        }

        @Override
        public void handle(EntityIndexingFailureContext entityIndexingFailureContext) {
        }
    }

    @SearchExtension
    public static class AnotherSearchFailureHandler implements FailureHandler {
        @Override
        public void handle(FailureContext failureContext) {
        }

        @Override
        public void handle(EntityIndexingFailureContext entityIndexingFailureContext) {
        }
    }

    @Test
    public void test() {
        Assertions.fail("Startup should have failed");
    }

}
