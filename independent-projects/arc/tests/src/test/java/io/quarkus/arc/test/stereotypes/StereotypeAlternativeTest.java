package io.quarkus.arc.test.stereotypes;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.arc.Arc;
import io.quarkus.arc.test.ArcTestContainer;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Stereotype;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class StereotypeAlternativeTest {

    @RegisterExtension
    public ArcTestContainer container = new ArcTestContainer(BeAlternative.class, BeAlternativeWithPriority.class,
            NonAlternative.class, IamAlternative.class, NotAtAllAlternative.class, IamAlternativeWithPriority.class,
            ToBeOverridenFoo.class, MockedFoo.class, MockedFooWithExplicitPriority.class, Mock.class);

    @Test
    public void testStereotype() {
        assertEquals("OK", Arc.container().instance(NonAlternative.class).get().getId());
        assertEquals("OK", Arc.container().instance(NotAtAllAlternative.class).get().getId());

        assertEquals(MockedFooWithExplicitPriority.class.getSimpleName(),
                Arc.container().instance(ToBeOverridenFoo.class).get().ping());
        assertEquals(MockedFoo.class.getSimpleName(), Arc.container().instance(MockedFoo.class).get().ping());
    }

    @Alternative
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeAlternative {
    }

    @Priority(1)
    @Alternative
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface BeAlternativeWithPriority {
    }

    @Dependent
    static class NonAlternative {

        public String getId() {
            return "NOK";
        }

    }

    @Priority(1)
    @BeAlternative
    static class IamAlternative extends NonAlternative {

        @Override
        public String getId() {
            return "OK";
        }

    }

    @Dependent
    static class NotAtAllAlternative {

        public String getId() {
            return "NOK";
        }

    }

    @BeAlternativeWithPriority
    static class IamAlternativeWithPriority extends NotAtAllAlternative {

        @Override
        public String getId() {
            return "OK";
        }

    }

    @Dependent
    static class ToBeOverridenFoo {

        public String ping() {
            return ToBeOverridenFoo.class.getSimpleName();
        }
    }

    @Mock
    // should not be selected because of lower priority (has 1)
    static class MockedFoo extends ToBeOverridenFoo {

        @Override
        public String ping() {
            return MockedFoo.class.getSimpleName();
        }

    }

    @Mock
    @Priority(2)
    static class MockedFooWithExplicitPriority extends ToBeOverridenFoo {

        @Override
        public String ping() {
            return MockedFooWithExplicitPriority.class.getSimpleName();
        }
    }

    /**
     * The built-in stereotype intended for use with mock beans injected in tests.
     */
    @Priority(1)
    @Dependent
    @Alternative
    @Stereotype
    @Target({ TYPE, METHOD, FIELD })
    @Retention(RUNTIME)
    public @interface Mock {

    }

}
