package io.quarkus.arc.test.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.enterprise.inject.Stereotype;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.properties.UnlessBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

public class UnlessBuildPropertyRepeatableStereotypeTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MatchingProperty.class, InheritableMatchingProperty.class, TransitiveMatchingProperty.class,
                            InheritableTransitiveMatchingProperty.class, MyService.class, MatchingPropertyMyService.class,
                            InheritableMatchingPropertyMyService.class, TransitiveMatchingPropertyMyService.class,
                            InheritableTransitiveMatchingPropertyMyService.class, MyServiceSimple.class,
                            MyServiceMatchingPropertyDirect.class, MyServiceMatchingPropertyTransitive.class,
                            MyServiceMatchingPropertyOnSuperclassNotInheritable.class,
                            MyServiceMatchingPropertyOnSuperclassInheritable.class,
                            MyServiceMatchingPropertyTransitiveOnSuperclassNotInheritable.class,
                            MyServiceMatchingPropertyTransitiveOnSuperclassInheritable.class, Producers.class))
            .overrideConfigKey("foo.bar", "baz")
            .overrideConfigKey("some.prop", "val");

    @Inject
    @Any
    Instance<MyService> services;

    @Test
    public void test() {
        Set<String> hello = services.stream().map(MyService::hello).collect(Collectors.toSet());
        Set<Object> expected = Set.of(
                MyServiceSimple.class.getSimpleName(),
                MyServiceMatchingPropertyOnSuperclassNotInheritable.class.getSimpleName(),
                MyServiceMatchingPropertyTransitiveOnSuperclassNotInheritable.class.getSimpleName(),
                Producers.SIMPLE);
        assertEquals(expected, hello);
    }

    @UnlessBuildProperty(name = "foo.bar", stringValue = "baz")
    @UnlessBuildProperty(name = "some.prop", stringValue = "none")
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface MatchingProperty {
    }

    @UnlessBuildProperty(name = "foo.bar", stringValue = "quux")
    @UnlessBuildProperty(name = "some.prop", stringValue = "val")
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableMatchingProperty {
    }

    @MatchingProperty
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TransitiveMatchingProperty {
    }

    @MatchingProperty
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableTransitiveMatchingProperty {
    }

    interface MyService {
        String hello();
    }

    @MatchingProperty
    static abstract class MatchingPropertyMyService implements MyService {
    }

    @InheritableMatchingProperty
    static abstract class InheritableMatchingPropertyMyService implements MyService {
    }

    @TransitiveMatchingProperty
    static abstract class TransitiveMatchingPropertyMyService implements MyService {
    }

    @InheritableTransitiveMatchingProperty
    static abstract class InheritableTransitiveMatchingPropertyMyService implements MyService {
    }

    @ApplicationScoped
    static class MyServiceSimple implements MyService {
        @Override
        public String hello() {
            return MyServiceSimple.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @MatchingProperty
    static class MyServiceMatchingPropertyDirect implements MyService {
        @Override
        public String hello() {
            return MyServiceMatchingPropertyDirect.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @TransitiveMatchingProperty
    static class MyServiceMatchingPropertyTransitive implements MyService {
        @Override
        public String hello() {
            return MyServiceMatchingPropertyTransitive.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceMatchingPropertyOnSuperclassNotInheritable extends MatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceMatchingPropertyOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceMatchingPropertyOnSuperclassInheritable extends InheritableMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceMatchingPropertyOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceMatchingPropertyTransitiveOnSuperclassNotInheritable extends TransitiveMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceMatchingPropertyTransitiveOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceMatchingPropertyTransitiveOnSuperclassInheritable
            extends InheritableTransitiveMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceMatchingPropertyTransitiveOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class Producers {
        static final String SIMPLE = "Producers.simple";
        static final String MATCHING_PROPERTY_DIRECT = "Producers.matchingPropertyDirect";
        static final String MATCHING_PROPERTY_TRANSITIVE = "Producers.matchingPropertyTransitive";

        @Produces
        @ApplicationScoped
        MyService simple() {
            return new MyService() {
                @Override
                public String hello() {
                    return SIMPLE;
                }
            };
        }

        @Produces
        @ApplicationScoped
        @MatchingProperty
        MyService matchingPropertyDirect() {
            return new MyService() {
                @Override
                public String hello() {
                    return MATCHING_PROPERTY_DIRECT;
                }
            };
        }

        @Produces
        @ApplicationScoped
        @TransitiveMatchingProperty
        MyService matchingPropertyTransitive() {
            return new MyService() {
                @Override
                public String hello() {
                    return MATCHING_PROPERTY_TRANSITIVE;
                }
            };
        }
    }
}
