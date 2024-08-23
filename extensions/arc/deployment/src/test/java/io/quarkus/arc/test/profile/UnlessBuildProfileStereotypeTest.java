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

import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.test.QuarkusUnitTest;

public class UnlessBuildProfileStereotypeTest {
    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(TestNever.class, InheritableTestNever.class, TransitiveTestNever.class,
                            InheritableTransitiveTestNever.class, MyService.class, TestNeverMyService.class,
                            InheritableTestNeverMyService.class, TransitiveTestNeverMyService.class,
                            InheritableTransitiveTestNeverMyService.class, MyServiceSimple.class,
                            MyServiceTestNeverDirect.class, MyServiceTestNeverTransitive.class,
                            MyServiceTestNeverOnSuperclassNotInheritable.class,
                            MyServiceTestNeverOnSuperclassInheritable.class,
                            MyServiceTestNeverTransitiveOnSuperclassNotInheritable.class,
                            MyServiceTestNeverTransitiveOnSuperclassInheritable.class, Producers.class));

    @Inject
    @Any
    Instance<MyService> services;

    @Test
    public void test() {
        Set<String> hello = services.stream().map(MyService::hello).collect(Collectors.toSet());
        Set<Object> expected = Set.of(
                MyServiceSimple.class.getSimpleName(),
                MyServiceTestNeverOnSuperclassNotInheritable.class.getSimpleName(),
                MyServiceTestNeverTransitiveOnSuperclassNotInheritable.class.getSimpleName(),
                Producers.SIMPLE);
        assertEquals(expected, hello);
    }

    @UnlessBuildProfile("test")
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestNever {
    }

    @UnlessBuildProfile("test")
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableTestNever {
    }

    @TestNever
    @Stereotype
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface TransitiveTestNever {
    }

    @TestNever
    @Stereotype
    @Inherited
    @Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InheritableTransitiveTestNever {
    }

    interface MyService {
        String hello();
    }

    @TestNever
    static abstract class TestNeverMyService implements MyService {
    }

    @InheritableTestNever
    static abstract class InheritableTestNeverMyService implements MyService {
    }

    @TransitiveTestNever
    static abstract class TransitiveTestNeverMyService implements MyService {
    }

    @InheritableTransitiveTestNever
    static abstract class InheritableTransitiveTestNeverMyService implements MyService {
    }

    @ApplicationScoped
    static class MyServiceSimple implements MyService {
        @Override
        public String hello() {
            return MyServiceSimple.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @TestNever
    static class MyServiceTestNeverDirect implements MyService {
        @Override
        public String hello() {
            return MyServiceTestNeverDirect.class.getSimpleName();
        }
    }

    @ApplicationScoped
    @TransitiveTestNever
    static class MyServiceTestNeverTransitive implements MyService {
        @Override
        public String hello() {
            return MyServiceTestNeverTransitive.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceTestNeverOnSuperclassNotInheritable extends TestNeverMyService {
        @Override
        public String hello() {
            return MyServiceTestNeverOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceTestNeverOnSuperclassInheritable extends InheritableTestNeverMyService {
        @Override
        public String hello() {
            return MyServiceTestNeverOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceTestNeverTransitiveOnSuperclassNotInheritable extends TransitiveTestNeverMyService {
        @Override
        public String hello() {
            return MyServiceTestNeverTransitiveOnSuperclassNotInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class MyServiceTestNeverTransitiveOnSuperclassInheritable extends InheritableTransitiveTestNeverMyService {
        @Override
        public String hello() {
            return MyServiceTestNeverTransitiveOnSuperclassInheritable.class.getSimpleName();
        }
    }

    @ApplicationScoped
    static class Producers {
        static final String SIMPLE = "Producers.simple";
        static final String TEST_NEVER_DIRECT = "Producers.testNeverDirect";
        static final String TEST_NEVER_TRANSITIVE = "Producers.testNeverTransitive";

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
        @TestNever
        MyService testNeverDirect() {
            return new MyService() {
                @Override
                public String hello() {
                    return TEST_NEVER_DIRECT;
                }
            };
        }

        @Produces
        @ApplicationScoped
        @TransitiveTestNever
        MyService testNeverTransitive() {
            return new MyService() {
                @Override
                public String hello() {
                    return TEST_NEVER_TRANSITIVE;
                }
            };
        }
    }
}
