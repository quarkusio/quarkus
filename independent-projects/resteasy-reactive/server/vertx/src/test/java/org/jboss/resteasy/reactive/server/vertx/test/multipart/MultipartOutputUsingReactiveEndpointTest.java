package org.jboss.resteasy.reactive.server.vertx.test.multipart;

import static org.junit.jupiter.api.Assertions.fail;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.inject.spi.DeploymentException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.vertx.test.framework.ResteasyReactiveUnitTest;
import org.jboss.resteasy.reactive.server.vertx.test.multipart.other.OtherPackageFormDataBase;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Using '@Produces(MediaType.MULTIPART_FORM_DATA)' is not compatible with Non Blocking endpoints.
 */
public class MultipartOutputUsingReactiveEndpointTest extends AbstractMultipartTest {

    @RegisterExtension
    static ResteasyReactiveUnitTest test = new ResteasyReactiveUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(MultipartOutputReactiveResource.class, OtherPackageFormDataBase.class))
            .setExpectedException(DeploymentException.class);;

    @Test
    public void test() {
        fail("Should never have been called");
    }

    @Path("/multipart/reactive")
    public static class MultipartOutputReactiveResource {

        @GET
        @Produces(MediaType.MULTIPART_FORM_DATA)
        public Multi<OtherPackageFormDataBase> simple() {
            return Multi.createFrom().items(new OtherPackageFormDataBase());
        }

    }

}
