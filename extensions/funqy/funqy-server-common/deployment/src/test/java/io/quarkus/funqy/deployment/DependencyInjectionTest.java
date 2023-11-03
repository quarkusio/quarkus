package io.quarkus.funqy.deployment;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.funqy.Funq;
import io.quarkus.test.QuarkusUnitTest;

public class DependencyInjectionTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(
                    (jar) -> jar.addClasses(GreetingFunction.class, GreetingService.class, FarewellFunction.class,
                            HiFunction.class, ByeFunction.class));

    @Inject
    GreetingFunction greetingFunction;

    @Inject
    FarewellFunction farewellFunction;

    @Inject
    ByeFunction byeFunction;

    @Inject
    HiFunction hiFunction;

    @Test
    public void testFieldInjection() {
        // bean with a pseudo-scope
        Assertions.assertEquals("Hello World!", greetingFunction.greeting());
        // normal scoped bean
        Assertions.assertEquals("Bye!", byeFunction.bye());
    }

    @Test
    public void testConstructorInjection() {
        // bean with a pseudo-scope
        Assertions.assertEquals("Goodbye!", farewellFunction.farewell());
        // normal scoped bean
        Assertions.assertEquals("Hi!", hiFunction.hi());
    }

    public static class GreetingFunction {

        @Inject
        GreetingService greetingService;

        @Funq
        public String greeting() {
            return greetingService.sayHello();
        }

    }

    public static class FarewellFunction {

        private final GreetingService greetingService;

        public FarewellFunction(GreetingService greetingService) {
            this.greetingService = greetingService;
        }

        @Funq
        public String farewell() {
            return greetingService.sayGoodbye();
        }

    }

    @ApplicationScoped
    public static class ByeFunction {

        @Inject
        GreetingService greetingService;

        @Funq
        public String bye() {
            return greetingService.sayBye();
        }

    }

    @ApplicationScoped
    public static class HiFunction {

        private final GreetingService greetingService;

        public HiFunction(GreetingService greetingService) {
            this.greetingService = greetingService;
        }

        @Funq
        public String hi() {
            return greetingService.sayHi();
        }

    }

    @Singleton
    public static class GreetingService {

        public String sayHello() {
            return "Hello World!";
        }

        public String sayGoodbye() {
            return "Goodbye!";
        }

        public String sayHi() {
            return "Hi!";
        }

        public String sayBye() {
            return "Bye!";
        }

    }

}
