package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.function.Supplier;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.reactive.MultipartForm;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ErroneousFieldMultipartInputTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
            .setArchiveProducer(new Supplier<JavaArchive>() {
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
