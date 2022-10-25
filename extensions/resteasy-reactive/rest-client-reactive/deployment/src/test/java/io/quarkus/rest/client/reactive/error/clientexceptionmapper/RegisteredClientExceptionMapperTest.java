package io.quarkus.rest.client.reactive.error.clientexceptionmapper;

import static io.quarkus.rest.client.reactive.RestClientTestUtil.setUrlForClass;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.quarkus.test.QuarkusUnitTest;

public class RegisteredClientExceptionMapperTest {

    @RegisterExtension
    static final QuarkusUnitTest TEST = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(ClientWithRegisteredLowPriorityMapper.class, ClientNoProviders.class,
                            Resource.class, Dto.class,
                            LowPriorityExceptionMapper.class, HighPriorityExceptionMapper.class,
                            DummyException.class, DummyException2.class, DummyException3.class)
                    .addAsResource(
                            new StringAsset(setUrlForClass(ClientNoProviders.class)
                                    + setUrlForClass(ClientWithRegisteredLowPriorityMapper.class)
                                    + setUrlForClass(ClientWithRegisteredHighPriorityMapper.class)
                                    + setUrlForClass(ClientWithRegisteredHighPriorityMapperAndSetPriority.class)),
                            "application.properties"));

    @RestClient
    ClientNoProviders clientNoProviders;

    @RestClient
    ClientWithRegisteredLowPriorityMapper clientWithRegisteredLowPriorityMapper;

    @RestClient
    ClientWithRegisteredHighPriorityMapper clientWithRegisteredHighPriorityMapper;

    @RestClient
    ClientWithRegisteredHighPriorityMapperAndSetPriority clientWithRegisteredHighPriorityMapperAndSetPriority;

    @BeforeEach
    void setUp() {
        DummyException.executionCount.set(0);
        DummyException2.executionCount.set(0);
        DummyException3.executionCount.set(0);
    }

    @Test
    void customExceptionMapperEngagesWhenNoExceptionMappersRegistered() {
        assertThrows(DummyException.class, clientNoProviders::get404);
        assertThat(DummyException.executionCount.get()).isEqualTo(1);
    }

    @Test
    void customExceptionMapperReturnsNull() {
        assertThrows(RuntimeException.class, clientNoProviders::get400);
        assertThat(DummyException.executionCount.get()).isEqualTo(0);
    }

    @Test
    void customExceptionMapperEngagesWhenRegisteredExceptionMapperHasLowerPriority() {
        assertThrows(DummyException.class, clientWithRegisteredLowPriorityMapper::get404);
        assertThat(DummyException.executionCount.get()).isEqualTo(1);
    }

    @Test
    void customExceptionMapperDotNotEngageWhenRegisteredExceptionMapperHasHigherPriority() {
        assertThrows(DummyException3.class, clientWithRegisteredHighPriorityMapper::get404);
        assertThat(DummyException3.executionCount.get()).isEqualTo(1);
    }

    @Test
    void customExceptionMapperEngagesWhenRegisteredExceptionMapperHasHigherPriority() {
        assertThrows(DummyException.class, clientWithRegisteredHighPriorityMapperAndSetPriority::get404);
        assertThat(DummyException.executionCount.get()).isEqualTo(1);
    }

    @Path("/error")
    @RegisterRestClient
    public interface ClientNoProviders {
        @Path("/404")
        @GET
        Dto get404();

        @Path("/400")
        @GET
        Dto get400();

        @ClientExceptionMapper
        static DummyException map(Response response) {
            if (response.getStatus() == 404) {
                return new DummyException();
            }
            return null;
        }
    }

    @Path("/error")
    @RegisterRestClient
    @RegisterProvider(LowPriorityExceptionMapper.class)
    public interface ClientWithRegisteredLowPriorityMapper {
        @Path("/404")
        @GET
        Dto get404();

        @Path("/400")
        @GET
        Dto get400();

        @ClientExceptionMapper
        static DummyException map(Response response) {
            if (response.getStatus() == 404) {
                return new DummyException();
            }
            return null;
        }
    }

    @Path("/error")
    @RegisterRestClient
    @RegisterProvider(HighPriorityExceptionMapper.class)
    public interface ClientWithRegisteredHighPriorityMapper {
        @Path("/404")
        @GET
        Dto get404();

        @Path("/400")
        @GET
        Dto get400();

        @ClientExceptionMapper
        static DummyException map(Response response) {
            if (response.getStatus() == 404) {
                return new DummyException();
            }
            return null;
        }
    }

    @Path("/error")
    @RegisterRestClient
    @RegisterProvider(HighPriorityExceptionMapper.class)
    public interface ClientWithRegisteredHighPriorityMapperAndSetPriority {
        @Path("/404")
        @GET
        Dto get404();

        @Path("/400")
        @GET
        Dto get400();

        @ClientExceptionMapper(priority = Priorities.USER - 100)
        static DummyException map(Response response) {
            if (response.getStatus() == 404) {
                return new DummyException();
            }
            return null;
        }
    }

}
