package io.quarkus.arc.test.profile;

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

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class IfBuildProfileStereotypeTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(DevOnly.class, InheritableDevOnly.class, TransitiveDevOnly.class,
                            InheritableTransitiveDevOnly.class, MyService.class, DevOnlyMyService.class,
                            InheritableDevOnlyMyService.class, TransitiveDevOnlyMyService.class,
                            InheritableTransitiveDevOnlyMyService.class, MyServiceSimple.class,
                            MyServiceDevOnlyDirect.class, MyServiceDevOnlyTransitive.class,
                            MyServiceDevOnlyOnSuperclassNotInheritable.class,
                            MyServiceDevOnlyOnSuperclassInheritable.class,
                            MyServiceDevOnlyTransitiveOnSuperclassNotInheritable.class,
                            MyServiceDevOnlyTransitiveOnSuperclassInheritable.class, Producers.class));

    @Inject
    @Any
    Instance<MyService> services;

    @Test
    public void test() {
        Set<String> hello = services.stream().map(MyService::hello).collect(Collectors.toSet());
        Set<Object> expected = Set.of(
                MyServiceSimple.class.getSimpleName(),
                MyServiceDevOnlyOnSuperclassNotInheritable.class.getSimpleName(),
                MyServiceDevOnlyTransitiveOnSuperclassNotInheritable.class.getSimpleName(),
                Producers.SIMPLE);
        assertEquals(expected, hello);
    }

    @IfBuildProfile("dev")
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface DevOnly {
    }

    @IfBuildProfile("dev")
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableDevOnly {
    }

    @DevOnly
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TransitiveDevOnly {
    }

    @DevOnly
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableTransitiveDevOnly {
    }

    interface MyService {
        String hello();
    }

    @DevOnly
    static abstract class DevOnlyMyService implements MyService {
    }

    @InheritableDevOnly
    static abstract class InheritableDevOnlyMyService implements MyService {
    }

    @TransitiveDevOnly
    static abstract class TransitiveDevOnlyMyService implements MyService {
    }

    @InheritableTransitiveDevOnly
    static abstract class InheritableTransitiveDevOnlyMyService implements MyService {
    }

    @ApplicationScoped
    static class MyServiceSimple implements MyService {
        @Override
        public String hello() {
            return MyServiceSimple.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @DevOnly
    static class MyServiceDevOnlyDirect implements MyService {
        @Override
        public String hello() {
            return MyServiceDevOnlyDirect.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @TransitiveDevOnly
    static class MyServiceDevOnlyTransitive implements MyService {
        @Override
        public String hello() {
            return MyServiceDevOnlyTransitive.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceDevOnlyOnSuperclassNotInheritable extends DevOnlyMyService {
        @Override
        public String hello() {
            return MyServiceDevOnlyOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceDevOnlyOnSuperclassInheritable extends InheritableDevOnlyMyService {
        @Override
        public String hello() {
            return MyServiceDevOnlyOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceDevOnlyTransitiveOnSuperclassNotInheritable extends TransitiveDevOnlyMyService {
        @Override
        public String hello() {
            return MyServiceDevOnlyTransitiveOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceDevOnlyTransitiveOnSuperclassInheritable extends InheritableTransitiveDevOnlyMyService {
        @Override
        public String hello() {
            return MyServiceDevOnlyTransitiveOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class Producers {
        static final String SIMPLE = "Producers.simple";
        static final String DEV_ONLY_DIRECT = "Producers.devOnlyDirect";
        static final String DEV_ONLY_TRANSITIVE = "Producers.devOnlyTransitive";

        @Produces
        MyService simple = new MyService() {
            @Override
            public String hello() {
                return SIMPLE;
            }
        };

        @Produces
        @DevOnly
        MyService devOnlyDirect = new MyService() {
            @Override
            public String hello() {
                return DEV_ONLY_DIRECT;
            }
        };

        @Produces
        @TransitiveDevOnly
        MyService devOnlyTransitive = new MyService() {
            @Override
            public String hello() {
                return DEV_ONLY_TRANSITIVE;
            }
        };
    }
}
