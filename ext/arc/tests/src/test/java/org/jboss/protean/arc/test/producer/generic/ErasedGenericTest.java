package org.jboss.protean.arc.test.producer.generic;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.ArcContainer;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.lang.annotation.Annotation;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Test for https://github.com/protean-project/shamrock/issues/120
 */
public class ErasedGenericTest {
    @Rule
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
