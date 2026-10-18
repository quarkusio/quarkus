package io.quarkus.resteasy.reactive.server.test.duplicate;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;

public class DuplicateResourceDifferentPathParamNameTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(DuplicateResourceDifferentPathParamNameResource.class))
            .assertException(throwable -> Assertions.assertThat(throwable).hasMessageContaining("is declared by"));

    @Test
    public void dummy() {

    }

    @Path("/api-path")
    public static class DuplicateResourceDifferentPathParamNameResource {

        @GET
        @Path("v1/{parentId}")
        public String getByParentId(@PathParam("parentId") Long parentId) {
            return "getByParentId";
        }

        @GET
        @Path("v1/{product}")
        public String getByProduct(@PathParam("product") Long product) {
            return "getByProduct";
        }
    }
}
