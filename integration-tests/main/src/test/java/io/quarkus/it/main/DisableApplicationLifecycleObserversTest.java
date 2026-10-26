package io.quarkus.it.main;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.it.arc.LifecycleObserversService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(DisableApplicationLifecycleObserversTest.DisableLifecycleObserversProfile.class)
class DisableApplicationLifecycleObserversTest {
    @Test
    void lifecycleObserversDisabled() {
        assertThat(LifecycleObserversService.STARTUP_METHOD_CALLED).isFalse();
        assertThat(LifecycleObserversService.STARTUP_EVENT_OBSERVED).isFalse();
    }

    public static class DisableLifecycleObserversProfile implements QuarkusTestProfile {
        @Override
        public boolean disableApplicationLifecycleObservers() {
            return true;
        }
    }
}
