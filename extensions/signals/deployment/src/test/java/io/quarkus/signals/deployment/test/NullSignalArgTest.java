package io.quarkus.signals.deployment.test;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.signals.Signal;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that passing a {@code null} signal object or a {@code null} response type
 * to any emission method throws {@link IllegalArgumentException}.
 */
public class NullSignalArgTest extends AbstractSignalTest {

    @RegisterExtension
    static final QuarkusExtensionTest test = new QuarkusExtensionTest()
            .withApplicationRoot(root -> root.addClasses(Cmd.class));

    @Inject
    Signal<Cmd> signal;

    @Test
    void publishNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.publish(null));
    }

    @Test
    void sendNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.send(null));
    }

    @Test
    void requestNullSignal() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.request(null, String.class));
    }

    @Test
    void requestNullSignalTypeLiteral() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.request(null, new TypeLiteral<String>() {
        }));
    }

    @Test
    void requestNullResponseType() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.request(new Cmd(), (Class<?>) null));
    }

    @Test
    void requestNullResponseTypeLiteral() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.request(new Cmd(), (TypeLiteral<?>) null));
    }

    @Test
    void reactivePublishNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.reactive().publish(null));
    }

    @Test
    void reactiveSendNull() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.reactive().send(null));
    }

    @Test
    void reactiveRequestNullSignal() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.reactive().request(null, String.class));
    }

    @Test
    void reactiveRequestNullSignalTypeLiteral() {
        assertThatIllegalArgumentException().isThrownBy(() -> signal.reactive().request(null, new TypeLiteral<String>() {
        }));
    }

    @Test
    void reactiveRequestNullResponseType() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> signal.reactive().request(new Cmd(), (Class<?>) null));
    }

    @Test
    void reactiveRequestNullResponseTypeLiteral() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> signal.reactive().request(new Cmd(), (TypeLiteral<?>) null));
    }

    record Cmd() {
    }
}
