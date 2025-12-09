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

public class EmptyBeanParamRecordTest {
    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .setArchiveProducer(() -> {
                return ShrinkWrap.create(JavaArchive.class)
                        .addClasses(EmptyBeanParam.class, Resource.class);
            }).assertException(x -> {
                x.printStackTrace();
                Assertions.assertEquals(
                        "Body parameters (or non-annotated fields) are not allowed for records. Make sure to annotate your record components with @Rest* or @*Param or that they can be injected as context objects.",
                        x.getMessage());
            });

    @Test
    void empty() {
        Assertions.fail();
    }

    public record EmptyBeanParam(String something, Integer other) {
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
