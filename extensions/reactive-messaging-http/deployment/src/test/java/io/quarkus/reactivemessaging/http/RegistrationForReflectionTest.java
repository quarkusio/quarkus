package io.quarkus.reactivemessaging.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.ProcessorBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.smallrye.reactivemessaging.deployment.ConnectorProviderBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

public class RegistrationForReflectionTest {

    private static volatile List<ReflectiveClassBuildItem> registeredClasses;

    @RegisterExtension
    static QuarkusUnitTest runner = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .addBuildChainCustomizer(buildCustomizer());

    @Test
    void shouldRegisterPayloadTypesForReflection() {
        // if it gets there, it succeeded
    }

    private static void checkProperClassesAreRegistered() {
        assertThat(registeredClasses)
                .overridingErrorMessage("buildCustomizer failed to collect registered classes")
                .isNotNull();

        List<String> allRegisteredClasses = registeredClasses.stream()
                .flatMap(c -> c.getClassNames().stream())
                .collect(Collectors.toList());

        List<String> expectedClasses = Stream
                .of(PT1.class, PT2.class, PT3.class, PT4.class, PT5.class, PT6.class, PT7.class,
                        PT8.class,
                        PT9.class, PT10.class, PT11.class, PT12.class)
                .map(Class::getName).collect(Collectors.toList());

        assertThat(allRegisteredClasses).containsAll(expectedClasses);
    }

    private static Consumer<BuildChainBuilder> buildCustomizer() {
        return new Consumer<BuildChainBuilder>() {
            @Override
            public void accept(BuildChainBuilder chainBuilder) {
                chainBuilder.addBuildStep(
                        new BuildStep() {
                            @Override
                            public void execute(BuildContext context) {
                                registeredClasses = context.consumeMulti(ReflectiveClassBuildItem.class);
                                checkProperClassesAreRegistered();
                            }
                        }).consumes(ReflectiveClassBuildItem.class)
                        .produces(GeneratedResourceBuildItem.class).build();
                chainBuilder.addBuildStep(new BuildStep() {
                    @Override
                    public void execute(BuildContext context) {
                        //this stops the endpoints being automatically started, to avoid having to configure paths
                        context.produce(new ConnectorProviderBuildItem("bogus-connector"));
                    }
                }).produces(ConnectorProviderBuildItem.class).build();
            }
        };
    }

    @ApplicationScoped
    static class ConsumerBean {
        @Incoming("c1")
        void consume(PT1 payload) {

        }

        @Incoming("c2")
        CompletionStage<Void> consume(Message<PT2> payload) {
            return new CompletableFuture<>();
        }

        @Incoming("c3")
        @Outgoing("c5")
        Multi<PT5> consume(Multi<PT3> payload) {
            return payload.onItem().transform(o -> new PT5());
        }

        @Incoming("c4")
        @Outgoing("c6")
        Multi<Message<PT6>> consumeMultiWithMessage(Multi<Message<PT4>> payload) {
            return payload.onItem().transform(msg -> Message.of(new PT6()));
        }

        @Incoming("c5")
        Subscriber<PT5> consumeWithSubscriber() {
            return createSubscriber();
        }

        @Incoming("c6")
        Subscriber<Message<PT6>> consumeWithSubscriberWithMessage() {
            return createSubscriber();
        }

        @Incoming("c7")
        SubscriberBuilder<PT7, Void> consumeWithSubscriberBuilder() {
            return ReactiveStreams.<PT7> builder().ignore();
        }

        @Incoming("c8")
        SubscriberBuilder<Message<PT8>, Void> consumeWithSubscriberBuilderOfMsg() {
            return ReactiveStreams.<Message<PT8>> builder().ignore();
        }

        @Incoming("c9")
        @Outgoing("c1")
        Processor<PT9, PT1> consumeWithProcessor() {
            return createProcessor(new PT1());
        }

        @Incoming("c10")
        @Outgoing("c2")
        Processor<Message<PT10>, PT2> consumeWithProcessorOfMsg() {
            return createProcessor(new PT2());
        }

        @Incoming("c11")
        @Outgoing("c3")
        ProcessorBuilder<PT11, PT3> consumeWithProcessorBuilder() {
            return ReactiveStreams.<PT11> builder().map(o -> new PT3());
        }

        @Incoming("c12")
        @Outgoing("c4")
        ProcessorBuilder<Message<PT12>, Message<PT4>> consumeWithProcessorBuilderOfMsg() {
            return ReactiveStreams.<Message<PT12>> builder().map(o -> Message.of(new PT4()));
        }

        @Outgoing("c7")
        Publisher<PT7> producer7() {
            return createPublisher(new PT7());
        }

        @Outgoing("c8")
        Publisher<PT8> producer8() {
            return createPublisher(new PT8());
        }

        @Outgoing("c9")
        Publisher<PT9> producer9() {
            return createPublisher(new PT9());
        }

        @Outgoing("c10")
        Publisher<PT10> producer10() {
            return createPublisher(new PT10());
        }

        @Outgoing("c11")
        Publisher<PT11> producer11() {
            return createPublisher(new PT11());
        }

        @Outgoing("c12")
        Publisher<PT12> producer12() {
            return createPublisher(new PT12());
        }

        <T> Publisher<T> createPublisher(T val) {
            return ReactiveStreams.of(val)
                    .buildRs();
        }

        <V, T> Processor<V, T> createProcessor(T val) {
            return ReactiveStreams.<V> builder()
                    .map(m -> val)
                    .buildRs();
        }

        Subscriber createSubscriber() {
            return ReactiveStreams.builder()
                    .buildRs();
        }
    }

    static class PT1 {
    }

    static class PT2 {
    }

    static class PT3 {
    }

    static class PT4 {
    }

    static class PT5 {
    }

    static class PT6 {
    }

    static class PT7 {
    }

    static class PT8 {
    }

    static class PT9 {
    }

    static class PT10 {
    }

    static class PT11 {
    }

    static class PT12 {
    }

}
