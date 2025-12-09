package io.quarkus.resteasy.reactive.server.test.beanparam;

import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class ReallyEmptyBeanParamRecordTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                return ShrinkWrap.create(JavaArchive.class)
                        .addClasses(EmptyBeanParam.class, Resource.class);
            }).assertException(x -> {
                x.printStackTrace();
                Assertions.assertEquals(
                        "No annotations found on fields at 'io.quarkus.resteasy.reactive.server.test.beanparam.ReallyEmptyBeanParamRecordTest$EmptyBeanParam'. Annotations like `@QueryParam` should be used in fields, not in methods.",
                        x.getMessage());
            });

    @Test
    void empty() {
        Assertions.fail();
    }

    // This one does not even have body params
    public record EmptyBeanParam() {
    }

    @Path("/")
    public static class Resource {

        @Path("/record")
        @GET
        public String beanParamRecord(@BeanParam EmptyBeanParam param) {
            return "OK";
        }
    }
}
