package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import static org.junit.jupiter.api.Assertions.fail;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.function.Supplier;
import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ErroneousFieldMultipartInputTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(new Supplier<>() {
                @Override
                public JavaArchive get() {
                    return ShrinkWrap.create(JavaArchive.class)
                            .addClasses(Input.class);
                }

            }).setExpectedException(IllegalArgumentException.class);

    @Test
    public void testSimple() {
        fail("Should never be called");
    }

    @Path("test")
    public static class TestEndpoint {

        @Produces(MediaType.TEXT_PLAIN)
        @Consumes(MediaType.MULTIPART_FORM_DATA)
        @POST
        public int test(@MultipartForm Input formData) {
            return formData.txtFile.length;
        }
    }

    public static class Input {
        @RestForm
        private String name;

        @RestForm
        public byte[] txtFile;
    }

}
