package io.quarkus.arc.test.clientproxy.finalmethod;

import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.UnproxyableResolutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;

public class FinalMethodIllegalWhenNotInjectedTest {

    @RegisterExtension
    public ArcTestContainer container = ArcTestContainer.builder()
            .beanClasses(Moo.class)
            .strictCompatibility(true)
            .build();

    @Test
    public void test() {
        assertThrows(UnproxyableResolutionException.class, () -> {
            Arc.container().instance(Moo.class).get();
        });
    }

    @ApplicationScoped
    static class Moo {
        final int getVal() {
            return -1;
        }
    }
}
