package io.quarkus.netty.runtime.graal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link TcnativeAbsent} guards the substitutions that hard-code the OpenSSL engine as unavailable: they must apply
 * only when netty-tcnative is not on the classpath.
 */
class TcnativeAbsentTest {

    @Test
    void absentWhenTheProbeClassCannotBeLoaded() {
        ClassLoader none = new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                throw new ClassNotFoundException(name);
            }
        };
        assertTrue(TcnativeAbsent.isAbsent(none));
    }

    @Test
    void presentWhenTheProbeClassLoads() {
        ClassLoader fake = new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (TcnativeAbsent.PROBE_CLASS.equals(name)) {
                    return Object.class; // any class will do: only its presence is probed
                }
                throw new ClassNotFoundException(name);
            }
        };
        assertFalse(TcnativeAbsent.isAbsent(fake));
    }

    @Test
    void absentWhenLoadingTheProbeClassFailsWithALinkageError() {
        ClassLoader broken = new ClassLoader(null) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) {
                throw new NoClassDefFoundError(name);
            }
        };
        assertTrue(TcnativeAbsent.isAbsent(broken));
    }

    @Test
    void thisModuleHasNoTcnativeOnItsClasspath() {
        // The netty extension itself must not pull tcnative in; it is an application-level, opt-in dependency.
        assertTrue(new TcnativeAbsent().getAsBoolean());
    }
}
