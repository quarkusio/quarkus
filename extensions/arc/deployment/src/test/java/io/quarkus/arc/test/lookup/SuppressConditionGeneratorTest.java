package io.quarkus.arc.test.lookup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.All;
import io.quarkus.arc.deployment.SuppressConditionGeneratorBuildItem;
import io.quarkus.arc.processor.BeanInfo;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.test.QuarkusExtensionTest;

public class SuppressConditionGeneratorTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(Service.class, Suppressible.class, AlsoSuppressible.class,
                            ServiceAlpha.class, ServiceBravo.class, ServiceCharlie.class, ServiceDelta.class,
                            Consumer.class))
            .addBuildChainCustomizer(b -> {
                // First generator: suppress beans annotated with @Suppressible
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new SuppressConditionGeneratorBuildItem(generation -> {
                            BeanInfo bean = generation.bean();
                            if (bean.getTarget().isPresent()
                                    && bean.getTarget().get().asClass()
                                            .hasAnnotation(DotName.createSimple(Suppressible.class))) {
                                generation.method().returnTrue();
                            }
                        }));
                    }
                }).produces(SuppressConditionGeneratorBuildItem.class).build();
                // Second generator: suppress beans annotated with @AlsoSuppressible
                b.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new SuppressConditionGeneratorBuildItem(generation -> {
                            BeanInfo bean = generation.bean();
                            if (bean.getTarget().isPresent()
                                    && bean.getTarget().get().asClass()
                                            .hasAnnotation(DotName.createSimple(AlsoSuppressible.class))) {
                                generation.method().returnTrue();
                            }
                        }));
                    }
                }).produces(SuppressConditionGeneratorBuildItem.class).build();
            });

    @Inject
    Consumer consumer;

    @Test
    public void testMultipleGenerators() {
        // ServiceAlpha is @Suppressible -> suppressed by the first generator
        // ServiceBravo is @AlsoSuppressible -> suppressed by the second generator
        // ServiceDelta is both @Suppressible and @AlsoSuppressible -> both generators contribute to isSuppressed()
        // ServiceCharlie has no suppression annotation -> available
        assertEquals(1, consumer.allServices.size());
        assertTrue(consumer.allServices.stream().noneMatch(s -> s instanceof ServiceAlpha));
        assertTrue(consumer.allServices.stream().noneMatch(s -> s instanceof ServiceBravo));
        assertTrue(consumer.allServices.stream().noneMatch(s -> s instanceof ServiceDelta));
        assertEquals("charlie", consumer.pingService());
    }

    interface Service {
        String ping();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Suppressible {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface AlsoSuppressible {
    }

    @Suppressible
    @Singleton
    static class ServiceAlpha implements Service {
        @Override
        public String ping() {
            return "alpha";
        }
    }

    @AlsoSuppressible
    @Singleton
    static class ServiceBravo implements Service {
        @Override
        public String ping() {
            return "bravo";
        }
    }

    @Suppressible
    @AlsoSuppressible
    @Singleton
    static class ServiceDelta implements Service {
        @Override
        public String ping() {
            return "delta";
        }
    }

    @Singleton
    static class ServiceCharlie implements Service {
        @Override
        public String ping() {
            return "charlie";
        }
    }

    @Singleton
    static class Consumer {

        @Inject
        Instance<Service> services;

        @Inject
        @All
        List<Service> allServices;

        String pingService() {
            return services.get().ping();
        }
    }
}
