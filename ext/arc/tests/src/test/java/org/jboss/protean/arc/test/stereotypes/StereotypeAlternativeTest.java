package org.jboss.protean.arc.test.stereotypes;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.UUID;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;

import org.jboss.protean.arc.Arc;
import org.jboss.protean.arc.test.ArcTestContainer;
import org.junit.Rule;
import org.junit.Test;

public class StereotypeAlternativeTest {

    @Rule
    public ArcTestContainer container = new ArcTestContainer(BeAlternative.class, NonAternative.class, IamAlternative.class);

    @Test
    public void testStereotype() {
        assertEquals("OK", Arc.container().instance(NonAternative.class).get().getId());
    }

    @Alternative
    @Documented
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeAlternative {
    }

    @Dependent
    static class NonAternative {

        private String id;

        @PostConstruct
        void init() {
            id = UUID.randomUUID().toString();
        }

        public String getId() {
            return id;
        }

    }

    @Priority(1)
    @BeAlternative
    static class IamAlternative extends NonAternative {

        @Override
        public String getId() {
            return "OK";
        }

    }

}
