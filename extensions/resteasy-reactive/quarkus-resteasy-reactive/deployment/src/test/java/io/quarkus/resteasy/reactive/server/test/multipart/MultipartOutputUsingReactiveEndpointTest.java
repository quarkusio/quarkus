package io.quarkus.resteasy.reactive.server.test.multipart;

import static org.junit.jupiter.api.Assertions.fail;

import javax.enterprise.inject.spi.DeploymentException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.resteasy.reactive.server.test.multipart.other.OtherPackageFormDataBase;
import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.mutiny.Multi;

/**
 * Using '@Produces(MediaType.MULTIPART_FORM_DATA)' is not compatible with Non Blocking endpoints.
 */
public class MultipartOutputUsingReactiveEndpointTest extends AbstractMultipartTest {

    @RegisterExtension
    static QuarkusUnitTest test = new QuarkusUnitTest()
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
