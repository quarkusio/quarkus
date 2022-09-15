package io.quarkus.it.panache.reactive;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkus.panache.common.Parameters;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DuplicateMethodTest {

    @Inject
    DuplicateRepository repository;

    @Test
    public void shouldNotDuplicateMethodsInRepository() {
        assertThat(repository.findById(1)).isNotNull();
    }

    @Test
    public void shouldNotDuplicateMethodsInEntity() {
        DuplicateEntity entity = DuplicateEntity.<DuplicateEntity> findById(1).await().indefinitely();
        assertThat(entity).isNotNull();
        entity.persist().await().indefinitely();
        DuplicateEntity.update("foo", Parameters.with("a", 1)).await().indefinitely();
    }
}
