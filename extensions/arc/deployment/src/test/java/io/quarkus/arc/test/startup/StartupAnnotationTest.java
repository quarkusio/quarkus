package io.quarkus.arc.test.startup;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.ObserverMethod;
import javax.inject.Singleton;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.arc.Unremovable;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.processor.AnnotationsTransformer;
import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.runtime.Startup;
import io.quarkus.test.QuarkusUnitTest;

public class StartupAnnotationTest {

    static final List<String> LOG = new CopyOnWriteArrayList<String>();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(StartMe.class, SingletonStartMe.class, DependentStartMe.class, ProducerStartMe.class))
            .addBuildChainCustomizer(buildCustomizer());

    static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {

            @Override
            public void accept(BuildChainBuilder builder) {
                builder.addBuildStep(new BuildStep() {

                    @Override
                    public void execute(BuildContext context) {
                        context.produce(new AnnotationsTransformerBuildItem(new AnnotationsTransformer() {

                            @Override
                            public boolean appliesTo(Kind kind) {
                                return AnnotationTarget.Kind.CLASS.equals(kind);
                            }

                            @Override
                            public void transform(TransformationContext context) {
                                if (context.getTarget().asClass().name().toString().endsWith("SingletonStartMe")) {
                                    context.transform().add(Startup.class).done();
                                }
                            }

                        }));
                    }
                }).produces(AnnotationsTransformerBuildItem.class).build();
            }
        };
    }

    @Test
    public void testStartup() {
        // StartMe, SingletonStartMe, ProducerStartMe, DependentStartMe
        assertEquals(14, LOG.size(), "Unexpected number of log messages: " + LOG);
        assertEquals("startMe_c", LOG.get(0));
        assertEquals("startMe_c", LOG.get(1));
        assertEquals("startMe_pc", LOG.get(2));
        assertEquals("singleton_c", LOG.get(3));
        assertEquals("singleton_pc", LOG.get(4));
        assertEquals("producer_pc", LOG.get(5));
        assertEquals("produce_long", LOG.get(6));
        assertEquals("producer_pd", LOG.get(7));
        assertEquals("producer_pc", LOG.get(8));
        assertEquals("produce_string", LOG.get(9));
        assertEquals("producer_pd", LOG.get(10));
        assertEquals("dependent_c", LOG.get(11));
        assertEquals("dependent_pc", LOG.get(12));
        assertEquals("dependent_pd", LOG.get(13));
    }

    // This component should be started first
    @Startup(ObserverMethod.DEFAULT_PRIORITY - 1)
    // @ApplicationScoped is added automatically
    static class StartMe {

        public StartMe() {
            // This constructor will be invoked 2x - for proxy and contextual instance
            LOG.add("startMe_c");
        }

        @PostConstruct
        void init() {
            LOG.add("startMe_pc");
        }

        @PreDestroy
        void destroy() {
            LOG.add("startMe_pd");
        }

    }

    // @Startup is added by an annotation transformer
    @Unremovable // only classes annotated with @Startup are made unremovable 
    @Singleton
    static class SingletonStartMe {

        public SingletonStartMe() {
            LOG.add("singleton_c");
        }

        @PostConstruct
        void init() {
            LOG.add("singleton_pc");
        }

        @PreDestroy
        void destroy() {
            LOG.add("singleton_pd");
        }

    }

    @Dependent
    @Startup(Integer.MAX_VALUE)
    static class DependentStartMe {

        public DependentStartMe() {
            LOG.add("dependent_c");
        }

        @PostConstruct
        void init() {
            LOG.add("dependent_pc");
        }

        @PreDestroy
        void destroy() {
            LOG.add("dependent_pd");
        }

    }

    static class ProducerStartMe {

        @Startup(Integer.MAX_VALUE - 1)
        @Produces
        String produceString() {
            LOG.add("produce_string");
            return "ok";
        }

        @Startup(Integer.MAX_VALUE - 2)
        @Produces
        Long produceLong() {
            LOG.add("produce_long");
            return 1l;
        }

        @PostConstruct
        void init() {
            LOG.add("producer_pc");
        }

        @PreDestroy
        void destroy() {
            LOG.add("producer_pd");
        }

    }

}
