package io.quarkus.resteasy.reactive.server.test.stress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.hamcrest.Matchers;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.MethodInfo;
import org.jboss.resteasy.reactive.common.model.ResourceClass;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.model.HandlerChainCustomizer;
import org.jboss.resteasy.reactive.server.model.ServerResourceMethod;
import org.jboss.resteasy.reactive.server.processor.scanning.MethodScanner;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.BuildChainBuilder;
import io.quarkus.builder.BuildContext;
import io.quarkus.builder.BuildStep;
import io.quarkus.resteasy.reactive.server.spi.MethodScannerBuildItem;
import io.quarkus.test.QuarkusUnitTest;
import io.restassured.RestAssured;

/**
 * Tests lots of suspends/resumes per request
 */
public class SuspendResumeStressTest {

    private static volatile ExecutorService executorService;

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .addBuildChainCustomizer(new Consumer<BuildChainBuilder>() {
                @Override
                public void accept(BuildChainBuilder buildChainBuilder) {
                    buildChainBuilder.addBuildStep(new BuildStep() {
                        @Override
                        public void execute(BuildContext context) {
                            context.produce(new MethodScannerBuildItem(new MethodScanner() {
                                @Override
                                public List<HandlerChainCustomizer> scan(MethodInfo method, ClassInfo actualEndpointClass,
                                        Map<String, Object> methodContext) {
                                    return Collections.singletonList(new Custom());
                                }
                            }));
                        }
                    }).produces(MethodScannerBuildItem.class).build();
                }
            })
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(HelloResource.class));

    @Test
    public void testSuspendResumeStressTest() {
        executorService = Executors.newFixedThreadPool(10);
        try {
            for (int i = 0; i < 100; ++i) {
                RestAssured.when().get("/hello").then().body(Matchers.is("hello"));
            }
        } finally {
            executorService.shutdownNow();
            executorService = null;
        }
    }

    @Path("hello")
    public static class HelloResource {

        @GET
        public String hello() {
            return "hello";
        }

    }

    public static class Custom implements HandlerChainCustomizer {
        @Override
        public List<ServerRestHandler> handlers(Phase phase, ResourceClass resourceClass, ServerResourceMethod resourceMethod) {
            List<ServerRestHandler> handlers = new ArrayList<>();
            for (int i = 0; i < 100; ++i) {
                handlers.add(new ResumeHandler());
            }
            return handlers;
        }
    }

    public static class ResumeHandler implements ServerRestHandler {

        @Override
        public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
            requestContext.suspend();
            executorService.execute(requestContext::resume);
        }
    }
}
