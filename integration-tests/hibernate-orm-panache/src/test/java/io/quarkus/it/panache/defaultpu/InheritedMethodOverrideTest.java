package io.quarkus.it.panache.defaultpu;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

/**
 * Tests that Panache bytecode enhancement does not overwrite user-defined method overrides
 * in superinterfaces (default methods) or superclasses (concrete methods).
 *
 * @see <a href="https://github.com/quarkusio/quarkus/issues/29296">GitHub issue #29296</a>
 */
@QuarkusTest
public class InheritedMethodOverrideTest {

    @Inject
    RepositoryWithSuperInterfaceOverride interfaceRepo;

    @Inject
    RepositoryWithSuperClassOverride classRepo;

    @Test
    void deleteAll_fromSuperInterface_shouldThrow() {
        assertThatThrownBy(() -> interfaceRepo.deleteAll())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(DontDeletePanacheRepository.PROHIBIT_DELETE_MESSAGE);
    }

    @Test
    void deleteAll_fromSuperClass_shouldThrow() {
        assertThatThrownBy(() -> classRepo.deleteAll())
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage(AbstractDontDeletePanacheRepository.PROHIBIT_DELETE_MESSAGE);
    }

    @Test
    @Transactional
    void count_fromSuperInterfaceWithAbstractRedeclaration_shouldStillWork() {
        // count() is re-declared as abstract in DontDeletePanacheRepository,
        // Panache should still generate the implementation
        long count = interfaceRepo.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }

    @Test
    @Transactional
    void count_fromSuperClassWithAbstractRedeclaration_shouldStillWork() {
        // count() is declared as abstract in AbstractDontDeletePanacheRepository,
        // Panache should still generate the implementation
        long count = classRepo.count();
        assertThat(count).isGreaterThanOrEqualTo(0);
    }
}
