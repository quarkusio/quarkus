package io.quarkus.opentelemetry.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.opentelemetry.deployment.common.TestSpanExporter;
import io.quarkus.test.QuarkusUnitTest;

public class JaxRsInterceptorTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(JaxRsBean.class)
                    .addClass(TestSpanExporter.class));

    @Inject
    JaxRsBean jaxRsBean;
    @Inject
    TestSpanExporter spanExporter;

    @AfterEach
    void tearDown() {
        spanExporter.reset();
    }

    @Test
    void span() {
        jaxRsBean.method1("john");
        //        List<SpanData> spanItems = spanExporter.getFinishedSpanItems(1);
        //        assertEquals("SpanBean.span", spanItems.get(0).getName());
        //        assertEquals(INTERNAL, spanItems.get(0).getKind());
    }

    @ApplicationScoped
    @Path("/jaxRsBean")
    public static class JaxRsBean {

        @Path("test1")
        @GET
        public Response method1(@PathParam("name1") String name1) {
            return Response.ok().build();
        }
    }
}
