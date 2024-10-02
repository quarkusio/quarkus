package io.quarkus.restclient.exception;

import static jakarta.ws.rs.core.MediaType.TEXT_PLAIN;
import static jakarta.ws.rs.core.Response.Status.OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import jakarta.inject.Singleton;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

/**
 * Tests client sub-resources
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SubResourceTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(SubResourceTest.class, ClientRootResource.class, ClientSubResource.class,
                            ServerResource.class, TestExceptionMapper.class, TestException.class)
                    .addAsResource(new StringAsset(ClientRootResource.class.getName() + "/mp-rest/url=${test.url}\n"),
                            "application.properties"));

    @RestClient
    ClientRootResource clientRootResource;

    /**
     * Creates a REST client with an attached exception mapper. The exception mapper will throw a {@link TestException}.
     * This test invokes a call to the root resource.
     *
     */
    @Test
    public void rootResourceExceptionMapper() {
        try (Response ignored = clientRootResource.fromRoot()) {
            fail("fromRoot() should have thrown a TestException");
        } catch (TestException expected) {
            assertEquals("RootResource failed on purpose", expected.getMessage());
        }
    }

    /**
     * Creates a REST client with an attached exception mapper. The exception mapper will throw a {@link TestException}.
     * This test invokes a call to the sub-resource. The sub-resource then invokes an additional call which should also
     * result in a {@link TestException} thrown.
     *
     * @throws Exception if a test error occurs
     */
    @Test
    public void subResourceExceptionMapper() throws Exception {
        try (Response ignored = clientRootResource.subResource().fromSub()) {
            fail("fromSub() should have thrown a TestException");
        } catch (TestException expected) {
            assertEquals("SubResource failed on purpose", expected.getMessage());
        }
    }

    /**
     * This test invokes a call to the sub-resource. The sub-resource then invokes an additional call which should
     * return the header value for {@code test-header}.
     *
     */
    @Test
    public void subResourceWithHeader() {
        try (Response response = clientRootResource.subResource().withHeader()) {
            assertEquals(OK, response.getStatusInfo());
            assertEquals("SubResourceHeader", response.readEntity(String.class));
        }
    }

    /**
     * This test invokes a call to the sub-resource. The sub-resource then invokes an additional call which should
     * return the header value for {@code test-global-header}.
     *
     * @throws Exception if a test error occurs
     */
    @Test
    public void subResourceWithGlobalHeader() throws Exception {
        try (Response response = clientRootResource.subResource().withGlobalHeader()) {
            assertEquals(OK, response.getStatusInfo());
            assertEquals("GlobalSubResourceHeader", response.readEntity(String.class));
        }
    }

    @RegisterRestClient
    @RegisterProvider(TestExceptionMapper.class)
    @Path("/root")
    public interface ClientRootResource {
        @Path("/sub")
        ClientSubResource subResource();

        @GET
        Response fromRoot() throws TestException;
    }

    @ClientHeaderParam(name = "test-global-header", value = "GlobalSubResourceHeader")
    @Produces(TEXT_PLAIN)
    public interface ClientSubResource {
        @GET
        Response fromSub() throws TestException;

        @GET
        @ClientHeaderParam(name = "test-header", value = "SubResourceHeader")
        @Path("/header")
        Response withHeader();

        @GET
        @Path("/global/header")
        Response withGlobalHeader();
    }

    @Path("/root")
    public static class ServerResource {
        @GET
        public Response fromRoot() {
            return Response.serverError().entity("RootResource failed on purpose").build();
        }

        @GET
        @Path("/sub")
        public Response fromSub() {
            return Response.serverError().entity("SubResource failed on purpose").build();
        }

        @GET
        @Path("/sub/header")
        public Response subHeader(@HeaderParam("test-header") final String value) {
            return Response.ok(value).build();
        }

        @GET
        @Path("/sub/global/header")
        public Response subGlobalHeader(@HeaderParam("test-global-header") final String value) {
            return Response.ok(value).build();
        }
    }

    @Singleton
    public static class TestExceptionMapper implements ResponseExceptionMapper<TestException> {
        @Override
        public TestException toThrowable(final Response response) {
            return new TestException(response.readEntity(String.class));
        }
    }

    public static class TestException extends RuntimeException {
        public TestException(final String msg) {
            super(msg);
        }
    }
}
