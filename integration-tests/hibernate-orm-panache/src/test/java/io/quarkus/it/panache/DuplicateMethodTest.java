package io.quarkus.it.panache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

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
        DuplicateEntity entity = DuplicateEntity.findById(1);
        assertThat(entity).isNotNull();
        assertThatCode(entity::persist).doesNotThrowAnyException();
        assertThatCode(() -> DuplicateEntity.update("foo", Parameters.with("a", 1)))
                .doesNotThrowAnyException();
    }
}
