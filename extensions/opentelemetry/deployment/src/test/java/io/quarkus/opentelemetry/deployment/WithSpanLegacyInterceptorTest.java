package io.quarkus.opentelemetry.deployment;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.api.trace.SpanKind.SERVER;
import static io.quarkus.opentelemetry.deployment.common.TestSpanExporter.getSpanByKindAndParentId;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.extension.annotations.SpanAttribute;
import io.opentelemetry.extension.annotations.WithSpan;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.opentelemetry.deployment.common.TestSpanExporterProvider;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.config.SmallRyeConfig;
import io.vertx.ext.web.Router;

public class WithSpanLegacyInterceptorTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(
                    () -> ShrinkWrap.create(JavaArchive.class)
                            .addClass(SpanBean.class)
                            .addClasses(TestSpanExporter.class, TestSpanExporterProvider.class)
                            .addAsManifestResource(
                                    "META-INF/services-config/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider",
                                    "services/io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider")
                            .addAsResource("resource-config/application.properties", "application.properties"));

    @Inject
    SpanBean spanBean;
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void span() {
        spanBean.span();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.span", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void spanName() {
        spanBean.spanName();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("name", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @Test
    void spanKind() {
        spanBean.spanKind();
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.spanKind", spanItems.get(0).getName());
        assertEquals(SERVER, spanItems.get(0).getKind());
    }

    @Test
    void spanArgs() {
        spanBean.spanArgs("argument");
        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        assertEquals("SpanBean.spanArgs", spanItems.get(0).getName());
        assertEquals(INTERNAL, spanItems.get(0).getKind());
        assertEquals("argument", spanItems.get(0).getAttributes().get(AttributeKey.stringKey("arg")));
    }

    @Test
    void spanChild() {
        spanBean.spanChild();
        List<SpanData> spans = spanExporter.getFinishedSpanItems(2);

        final SpanData parent = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        assertEquals("SpanBean.spanChild", parent.getName());

        final SpanData child = getSpanByKindAndParentId(spans, INTERNAL, parent.getSpanId());
        assertEquals("SpanChildBean.spanChild", child.getName());
    }

    @Test
    void spanCdiRest() {
        spanBean.spanRestClient();
        List<SpanData> spans = spanExporter.getFinishedSpanItems(4);

        final SpanData parent = getSpanByKindAndParentId(spans, INTERNAL, "0000000000000000");
        final SpanData child = getSpanByKindAndParentId(spans, INTERNAL, parent.getSpanId());
        final SpanData client = getSpanByKindAndParentId(spans, CLIENT, child.getSpanId());
        final SpanData server = getSpanByKindAndParentId(spans, SERVER, client.getSpanId());
    }

    @ApplicationScoped
    public static class SpanBean {
        @WithSpan
        public void span() {

        }

        @WithSpan("name")
        public void spanName() {

        }

        @WithSpan(kind = SERVER)
        public void spanKind() {

        }

        @WithSpan
        public void spanArgs(@SpanAttribute(value = "arg") String arg) {

        }

        @Inject
        SpanChildBean spanChildBean;

        @WithSpan
        public void spanChild() {
            spanChildBean.spanChild();
        }

        @Inject
        SpanRestClient spanRestClient;

        @WithSpan
        public void spanRestClient() {
            spanRestClient.spanRestClient();
        }
    }

    @ApplicationScoped
    public static class SpanChildBean {
        @WithSpan
        public void spanChild() {

        }
    }

    @ApplicationScoped
    public static class SpanRestClient {
        @Inject
        SmallRyeConfig config;

        @WithSpan
        public void spanRestClient() {
            WebTarget target = ClientBuilder.newClient()
                    .target(UriBuilder.fromUri(config.getRawValue("test.url")).path("hello"));
            Response response = target.request().get();
            assertEquals(HTTP_OK, response.getStatus());
        }
    }

    @ApplicationScoped
    public static class HelloRouter {
        @Inject
        Router router;

        public void register(@Observes StartupEvent ev) {
            router.get("/hello").handler(rc -> rc.response().end("hello"));
        }
    }
}
