package io.quarkus.it.panache.kotlin

import io.quarkus.it.panache.reactive.kotlin.DuplicateEntity
import io.quarkus.it.panache.reactive.kotlin.DuplicateRepository
import io.quarkus.panache.common.Parameters
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Uni
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import javax.inject.Inject

@QuarkusTest
class DuplicateMethodTest {
    @Inject
    lateinit var repository: DuplicateRepository

    @Test
    fun shouldNotDuplicateMethodsInRepository() {
        Assertions.assertThat(repository.findById(1)).isNotNull()
    }

    @Test
    fun shouldNotDuplicateMethodsInEntity() {
        val entity: DuplicateEntity = DuplicateEntity.findById(1).await().indefinitely()
        Assertions.assertThat(entity).isNotNull()
        entity.persist<DuplicateEntity>().await().indefinitely()
        DuplicateEntity.update("foo", Parameters.with("a", 1)).await().indefinitely()
    }
}
