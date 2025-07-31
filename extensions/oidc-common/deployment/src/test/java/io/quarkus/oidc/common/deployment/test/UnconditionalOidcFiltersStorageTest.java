package io.quarkus.oidc.common.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.test.QuarkusUnitTest;

public class UnconditionalOidcFiltersStorageTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(RequestFilterOidcEndpointAll.class, ResponseFilterOidcEndpointAll.class,
                            RequestFilterNoEndpoint.class, ResponseFilterNoEndpoint.class, FilterProducer.class,
                            FilterProducer.OidcResponseFilterFieldProducer.class,
                            FilterProducer.OidcResponseFilterMethodProducer.class,
                            FilterProducer.OidcRequestFilterFieldProducer.class,
                            FilterProducer.OidcRequestFilterMethodProducer.class,
                            RequestFilterOidcEndpointAll2.class, ResponseFilterOidcEndpointAll2.class,
                            DummyOidcResponseFilter.class, DummyOidcRequestFilter.class,
                            RequestFilterOidcEndpointDiscovery.class, ResponseFilterOidcEndpointDiscovery.class,
                            RequestFilterOidcEndpointToken.class, ResponseFilterOidcEndpointToken.class,
                            ResponseFilterOidcEndpointTokenRevocation.class, RequestFilterOidcEndpointTokenRevocation.class,
                            ResponseFilterOidcEndpointIntrospection.class, RequestFilterOidcEndpointIntrospection.class));

    @Inject
    OidcFilterStorage filterStorage;

    @Test
    void testRetrievingRequestFilters_All() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.ALL);
        assertEquals(5, requestFilters.size());
        assertRequestFiltersAppliedToAllEndpoints(requestFilters);
    }

    @Test
    void testRetrievingResponseFilters_All() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.ALL);
        assertEquals(5, responseFilters.size());
        assertResponseFiltersAppliedToAllEndpoints(responseFilters);
    }

    @Test
    void testRetrievingRequestFilters_Discovery() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.DISCOVERY);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointDiscovery.class::isInstance);
        assertEquals(6, requestFilters.size());
        assertRequestFiltersAppliedToAllEndpoints(requestFilters);
    }

    @Test
    void testRetrievingResponseFilters_Discovery() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.DISCOVERY);
        assertEquals(6, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointDiscovery.class::isInstance);
        assertResponseFiltersAppliedToAllEndpoints(responseFilters);
    }

    @Test
    void testRetrievingRequestFilters_Token() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.TOKEN);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointToken.class::isInstance);
        assertEquals(6, requestFilters.size());
        assertRequestFiltersAppliedToAllEndpoints(requestFilters);
    }

    @Test
    void testRetrievingResponseFilters_Token() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.TOKEN);
        assertEquals(6, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointToken.class::isInstance);
        assertResponseFiltersAppliedToAllEndpoints(responseFilters);
    }

    @Test
    void testRetrievingRequestFilters_TokenRevocation() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.TOKEN_REVOCATION);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointTokenRevocation.class::isInstance);
        assertEquals(6, requestFilters.size());
        assertRequestFiltersAppliedToAllEndpoints(requestFilters);
    }

    @Test
    void testRetrievingResponseFilters_TokenRevocation() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.TOKEN_REVOCATION);
        assertEquals(6, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointTokenRevocation.class::isInstance);
        assertResponseFiltersAppliedToAllEndpoints(responseFilters);
    }

    @Test
    void testRetrievingRequestFilters_Introspection() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.INTROSPECTION);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointIntrospection.class::isInstance);
        assertEquals(6, requestFilters.size());
        assertRequestFiltersAppliedToAllEndpoints(requestFilters);
    }

    @Test
    void testRetrievingResponseFilters_Introspection() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.INTROSPECTION);
        assertEquals(6, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointIntrospection.class::isInstance);
        assertResponseFiltersAppliedToAllEndpoints(responseFilters);
    }

    private static void assertResponseFiltersAppliedToAllEndpoints(List<OidcResponseFilter> responseFilters) {
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointAll.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointAll2.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterNoEndpoint.class::isInstance);
        assertThat(responseFilters).anyMatch(FilterProducer.OidcResponseFilterFieldProducer.class::isInstance);
        assertThat(responseFilters).anyMatch(FilterProducer.OidcResponseFilterMethodProducer.class::isInstance);
    }

    private static void assertRequestFiltersAppliedToAllEndpoints(List<OidcRequestFilter> requestFilters) {
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointAll.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointAll2.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterNoEndpoint.class::isInstance);
        assertThat(requestFilters).anyMatch(FilterProducer.OidcRequestFilterFieldProducer.class::isInstance);
        assertThat(requestFilters).anyMatch(FilterProducer.OidcRequestFilterMethodProducer.class::isInstance);
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.INTROSPECTION)
    public static class RequestFilterOidcEndpointIntrospection extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.INTROSPECTION)
    public static class ResponseFilterOidcEndpointIntrospection extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.TOKEN_REVOCATION)
    public static class RequestFilterOidcEndpointTokenRevocation extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.TOKEN_REVOCATION)
    public static class ResponseFilterOidcEndpointTokenRevocation extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.TOKEN)
    public static class RequestFilterOidcEndpointToken extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.TOKEN)
    public static class ResponseFilterOidcEndpointToken extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.DISCOVERY)
    public static class RequestFilterOidcEndpointDiscovery extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.DISCOVERY)
    public static class ResponseFilterOidcEndpointDiscovery extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.ALL)
    public static class RequestFilterOidcEndpointAll2 extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.ALL)
    public static class ResponseFilterOidcEndpointAll2 extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.ALL)
    public static class RequestFilterOidcEndpointAll extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.ALL)
    public static class ResponseFilterOidcEndpointAll extends DummyOidcResponseFilter {
    }

    @Dependent
    public static class RequestFilterNoEndpoint extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    public static class ResponseFilterNoEndpoint extends DummyOidcResponseFilter {
    }

    @Singleton
    public static class FilterProducer {

        static class OidcResponseFilterFieldProducer extends DummyOidcResponseFilter {
        }

        static class OidcResponseFilterMethodProducer extends DummyOidcResponseFilter {
        }

        @Produces
        final OidcResponseFilter oidcResponseFilter = new OidcResponseFilterFieldProducer();

        @Produces
        OidcResponseFilter oidcResponseFilter() {
            return new OidcResponseFilterMethodProducer();
        }

        static class OidcRequestFilterFieldProducer extends DummyOidcRequestFilter {
        }

        static class OidcRequestFilterMethodProducer extends DummyOidcRequestFilter {

        }

        @Produces
        final OidcRequestFilter oidcRequestFilter = new OidcRequestFilterFieldProducer();

        @Produces
        OidcRequestFilter oidcRequestFilter() {
            return new OidcRequestFilterMethodProducer();
        }
    }
}
