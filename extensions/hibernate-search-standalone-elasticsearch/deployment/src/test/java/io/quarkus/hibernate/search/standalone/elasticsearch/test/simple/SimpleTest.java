package io.quarkus.hibernate.search.standalone.elasticsearch.test.simple;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.context.control.ActivateRequestContext;
import jakarta.inject.Inject;

import org.hibernate.search.engine.common.EntityReference;
import org.hibernate.search.mapper.pojo.standalone.entity.SearchIndexedEntity;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class SimpleTest {

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClass(MyEntity.class)
                    .addAsResource("application.properties"));

    @Inject
    SearchMapping mapping;

    @Test
    public void testMapping() {
        assertThat(mapping.allIndexedEntities())
                .hasSize(1)
                .element(0)
                .returns(MyEntity.class, SearchIndexedEntity::javaClass);
    }

    @Test
    @ActivateRequestContext
    public void testSession() {
        MyEntity entity = new MyEntity(42L, "someText");
        try (var session = mapping.createSession()) {
            session.indexingPlan().add(entity);
        }
        try (var session = mapping.createSession()) {
            assertThat(session.search(MyEntity.class)
                    .selectEntityReference()
                    .where(f -> f.matchAll())
                    .fetchHits(20))
                    .hasSize(1)
                    .element(0)
                    .returns(entity.getId(), EntityReference::id);
        }
    }
}
