package io.quarkus.arc.test.producer.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Annotation;
import java.util.Optional;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.arc.test.ArcTestContainer;

/**
 * Test for https://github.com/quarkusio/quarkus/issues/120
 */
public class ErasedGenericTest {
    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(ErasedTypeProducer.class, Claim.class, Target.class);

    @Test
    public void testPrimitiveProducers() {
        ArcContainer arc = Arc.container();
        Target target = arc.instance(Target.class).get();
        assertEquals("something", target.getSomething());
    }

    @Singleton
    static class Target {
        @Inject
        @Claim("something")
        Optional<String> optionalString;

        public String getSomething() {
            return optionalString.get();
        }
    }

    @Dependent
    static class ErasedTypeProducer {
        @Produces
        @Claim("")
        public Optional<String> getStringValue(InjectionPoint ip) {
            String name = getName(ip);
            Optional<String> value = Optional.of(name);
            return value;
        }

        @Produces
        @Claim("")
        @Named("RawClaimTypeProducer#getOptionalValue")
        public Optional getOptionalValue(InjectionPoint ip) {
            String name = getName(ip);
            Optional<Object> value = Optional.of(name);
            return value;
        }

        String getName(InjectionPoint ip) {
            String name = null;
            for (Annotation ann : ip.getQualifiers()) {
                if (ann instanceof Claim) {
                    Claim claim = (Claim) ann;
                    name = claim.value();
                }
            }
            return name;
        }

    }
}
