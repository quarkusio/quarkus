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

import io.quarkus.arc.properties.IfBuildProperty;
import io.quarkus.test.QuarkusUnitTest;

public class IfBuildPropertyRepeatableStereotypeTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(NotMatchingProperty.class, InheritableNotMatchingProperty.class,
                            TransitiveNotMatchingProperty.class,
                            InheritableTransitiveNotMatchingProperty.class, MyService.class, NotMatchingPropertyMyService.class,
                            InheritableNotMatchingPropertyMyService.class, TransitiveNotMatchingPropertyMyService.class,
                            InheritableTransitiveNotMatchingPropertyMyService.class, MyServiceSimple.class,
                            MyServiceNotMatchingPropertyDirect.class, MyServiceNotMatchingPropertyTransitive.class,
                            MyServiceNotMatchingPropertyOnSuperclassNotInheritable.class,
                            MyServiceNotMatchingPropertyOnSuperclassInheritable.class,
                            MyServiceNotMatchingPropertyTransitiveOnSuperclassNotInheritable.class,
                            MyServiceNotMatchingPropertyTransitiveOnSuperclassInheritable.class, Producers.class))
            .overrideConfigKey("foo.bar", "quux")
            .overrideConfigKey("some.prop", "val");

    @Inject
    @Any
    Instance<MyService> services;

    @Test
    public void test() {
        Set<String> hello = services.stream().map(MyService::hello).collect(Collectors.toSet());
        Set<Object> expected = Set.of(
                MyServiceSimple.class.getSimpleName(),
                MyServiceNotMatchingPropertyOnSuperclassNotInheritable.class.getSimpleName(),
                MyServiceNotMatchingPropertyTransitiveOnSuperclassNotInheritable.class.getSimpleName(),
                Producers.SIMPLE);
        assertEquals(expected, hello);
    }

    @IfBuildProperty(name = "foo.bar", stringValue = "baz")
    @IfBuildProperty(name = "some.prop", stringValue = "val")
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface NotMatchingProperty {
    }

    @IfBuildProperty(name = "foo.bar", stringValue = "quux")
    @IfBuildProperty(name = "some.prop", stringValue = "none")
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableNotMatchingProperty {
    }

    @NotMatchingProperty
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TransitiveNotMatchingProperty {
    }

    @NotMatchingProperty
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableTransitiveNotMatchingProperty {
    }

    interface MyService {
        String hello();
    }

    @NotMatchingProperty
    static abstract class NotMatchingPropertyMyService implements MyService {
    }

    @InheritableNotMatchingProperty
    static abstract class InheritableNotMatchingPropertyMyService implements MyService {
    }

    @TransitiveNotMatchingProperty
    static abstract class TransitiveNotMatchingPropertyMyService implements MyService {
    }

    @InheritableTransitiveNotMatchingProperty
    static abstract class InheritableTransitiveNotMatchingPropertyMyService implements MyService {
    }

    @ApplicationScoped
    static class MyServiceSimple implements MyService {
        @Override
        public String hello() {
            return MyServiceSimple.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @NotMatchingProperty
    static class MyServiceNotMatchingPropertyDirect implements MyService {
        @Override
        public String hello() {
            return MyServiceNotMatchingPropertyDirect.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @TransitiveNotMatchingProperty
    static class MyServiceNotMatchingPropertyTransitive implements MyService {
        @Override
        public String hello() {
            return MyServiceNotMatchingPropertyTransitive.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceNotMatchingPropertyOnSuperclassNotInheritable extends NotMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceNotMatchingPropertyOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceNotMatchingPropertyOnSuperclassInheritable extends InheritableNotMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceNotMatchingPropertyOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceNotMatchingPropertyTransitiveOnSuperclassNotInheritable
            extends TransitiveNotMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceNotMatchingPropertyTransitiveOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceNotMatchingPropertyTransitiveOnSuperclassInheritable
            extends InheritableTransitiveNotMatchingPropertyMyService {
        @Override
        public String hello() {
            return MyServiceNotMatchingPropertyTransitiveOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class Producers {
        static final String SIMPLE = "Producers.simple";
        static final String NOT_MATCHING_PROPERTY_DIRECT = "Producers.notMatchingPropertyDirect";
        static final String NOT_MATCHING_PROPERTY_TRANSITIVE = "Producers.notMatchingPropertyTransitive";

        @Produces
        MyService simple = new MyService() {
            @Override
            public String hello() {
                return SIMPLE;
            }
        };

        @Produces
        @NotMatchingProperty
        MyService notMatchingPropertyDirect = new MyService() {
            @Override
            public String hello() {
                return NOT_MATCHING_PROPERTY_DIRECT;
            }
        };

        @Produces
        @TransitiveNotMatchingProperty
        MyService notMatchingPropertyTransitive = new MyService() {
            @Override
            public String hello() {
                return NOT_MATCHING_PROPERTY_TRANSITIVE;
            }
        };
    }
}
