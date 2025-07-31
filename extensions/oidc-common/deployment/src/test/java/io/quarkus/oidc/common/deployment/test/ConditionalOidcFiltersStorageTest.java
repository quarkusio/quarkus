package io.quarkus.oidc.common.deployment.test;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.DotName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcRequestFilter;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.common.deployment.OidcFilterPredicateBuildItem;
import io.quarkus.oidc.common.runtime.OidcFilterStorage;
import io.quarkus.test.QuarkusUnitTest;
import io.vertx.mutiny.core.buffer.Buffer;

public class ConditionalOidcFiltersStorageTest {

    @RegisterExtension
    static final QuarkusUnitTest test = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(DummyOidcResponseFilter.class, DummyOidcRequestFilter.class,
                            RequestFilterOidcEndpointJWKS.class, ResponseFilterOidcEndpointJWKS.class,
                            RequestFilterOidcEndpointUserInfo.class, ResponseFilterOidcEndpointUserInfo.class,
                            ResponseFilterOidcEndpointClientRegistration.class,
                            ResponseFilterJwksUserInfoClientRegistration.class, OnlyCodeBearerAuthPrettyPleasePredicate.class,
                            RequestFilterOidcEndpointClientRegistration.class, ResponseFilterOidcEndpointRegisteredClient.class,
                            RequestFilterOidcEndpointRegisteredClient.class, ConditionalRequestFilterWithOidcEndpoint.class,
                            RequestFilterRegisteredClientClientRegistration.class, OnlyCodeFlowPrettyPlease.class,
                            OnlyCodeFlowPrettyPleasePredicate.class, OidcFilterPredicateBuildItem.class, DotName.class,
                            OnlyBearerAuthPrettyPlease.class, EndWithErPrettyPleasePredicate.class, EndWithErPrettyPlease.class,
                            ResponseFilterWith2Conditions.class, RequestFilterWithCondition1.class,
                            RequestFilterWithCondition2.class))
            .addBuildChainCustomizer(builder -> builder
                    .addBuildStep(bc -> bc.produce(
                            OidcFilterPredicateBuildItem.requestFilter(DotName.createSimple(OnlyCodeFlowPrettyPlease.class),
                                    OnlyCodeFlowPrettyPleasePredicate.class.getName())))
                    .produces(OidcFilterPredicateBuildItem.class)
                    .build()
                    .addBuildStep(bc -> bc.produce(
                            OidcFilterPredicateBuildItem.responseFilter(DotName.createSimple(OnlyBearerAuthPrettyPlease.class),
                                    OnlyCodeBearerAuthPrettyPleasePredicate.class.getName())))
                    .produces(OidcFilterPredicateBuildItem.class)
                    .build()
                    .addBuildStep(bc -> bc.produce(
                            OidcFilterPredicateBuildItem.responseFilter(DotName.createSimple(EndWithErPrettyPlease.class),
                                    EndWithErPrettyPleasePredicate.class.getName())))
                    .produces(OidcFilterPredicateBuildItem.class)
                    .build());

    @Inject
    OidcFilterStorage filterStorage;

    @Test
    void testConditionalRequestFilter_RegisteredClient() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.REGISTERED_CLIENT);
        assertThat(requestFilters).noneMatch(ConditionalRequestFilterWithOidcEndpoint.class::isInstance);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.REGISTERED_CLIENT, null);
        assertThat(requestFilters).noneMatch(ConditionalRequestFilterWithOidcEndpoint.class::isInstance);
        var wrongContext = new OidcRequestFilter.OidcRequestContext(null, Buffer.buffer("whatever"), null);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.REGISTERED_CLIENT, wrongContext);
        assertThat(requestFilters).noneMatch(ConditionalRequestFilterWithOidcEndpoint.class::isInstance);
        var rightContext = new OidcRequestFilter.OidcRequestContext(null, Buffer.buffer("only-code-flow"), null);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.REGISTERED_CLIENT, rightContext);
        assertThat(requestFilters).anyMatch(ConditionalRequestFilterWithOidcEndpoint.class::isInstance);
        // 2 unconditional filters + 1 conditional filter
        assertEquals(3, requestFilters.size());
    }

    @Test
    void testConditionalResponseFilter_ClientRegistrationAndJWKS() {
        // this filter has 2 conditions, both must be met
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION, null);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        var ctxMatchingNoCondition = new OidcResponseFilter.OidcResponseContext(null, -1, null, Buffer.buffer(""));
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION, ctxMatchingNoCondition);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        var ctxMatchingJust1Condition = new OidcResponseFilter.OidcResponseContext(null, -1, null, Buffer.buffer("bearer"));
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION,
                ctxMatchingJust1Condition);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        var ctxMatchingBothConditions = new OidcResponseFilter.OidcResponseContext(null, 200, null, Buffer.buffer("bearer"));
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION,
                ctxMatchingBothConditions);
        assertThat(responseFilters).anyMatch(ResponseFilterWith2Conditions.class::isInstance);
        // 2 unconditional filters + 1 conditional filter
        assertEquals(3, responseFilters.size());
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS, null);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS, ctxMatchingNoCondition);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS, ctxMatchingJust1Condition);
        assertThat(responseFilters).noneMatch(ResponseFilterWith2Conditions.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS, ctxMatchingBothConditions);
        assertThat(responseFilters).anyMatch(ResponseFilterWith2Conditions.class::isInstance);
        // 2 unconditional filters + 1 conditional filter
        assertEquals(3, responseFilters.size());
    }

    @Test
    void test2ConditionalRequestFilters_UserInfo() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.USERINFO);
        assertThat(requestFilters).noneMatch(RequestFilterWithCondition1.class::isInstance);
        assertThat(requestFilters).noneMatch(RequestFilterWithCondition2.class::isInstance);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.USERINFO, null);
        assertThat(requestFilters).noneMatch(RequestFilterWithCondition1.class::isInstance);
        assertThat(requestFilters).noneMatch(RequestFilterWithCondition2.class::isInstance);
        var wrongContext = new OidcRequestFilter.OidcRequestContext(null, Buffer.buffer("whatever"), null);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.USERINFO, wrongContext);
        assertThat(requestFilters).noneMatch(RequestFilterWithCondition1.class::isInstance);
        assertThat(requestFilters).noneMatch(RequestFilterWithCondition2.class::isInstance);
        var rightContext = new OidcRequestFilter.OidcRequestContext(null, Buffer.buffer("only-code-flow"), null);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.USERINFO, rightContext);
        assertThat(requestFilters).anyMatch(RequestFilterWithCondition1.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterWithCondition2.class::isInstance);
        // 1 unconditional filter + 2 conditional filters
        assertEquals(3, requestFilters.size());
    }

    @Test
    void testRetrievingRequestFilters_JWKS() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.JWKS);
        assertEquals(1, requestFilters.size());
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointJWKS.class::isInstance);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.JWKS, null);
        assertEquals(1, requestFilters.size());
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointJWKS.class::isInstance);
    }

    @Test
    void testRetrievingResponseFilters_JWKS() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS);
        assertEquals(2, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointJWKS.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterJwksUserInfoClientRegistration.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.JWKS, null);
        assertEquals(2, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointJWKS.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterJwksUserInfoClientRegistration.class::isInstance);
    }

    @Test
    void testRetrievingRequestFilters_UserInfo() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.USERINFO);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointUserInfo.class::isInstance);
        assertEquals(1, requestFilters.size());
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.USERINFO, null);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointUserInfo.class::isInstance);
        assertEquals(1, requestFilters.size());
    }

    @Test
    void testRetrievingResponseFilters_UserInfo() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.USERINFO);
        assertEquals(2, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointUserInfo.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterJwksUserInfoClientRegistration.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.USERINFO, null);
        assertEquals(2, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointUserInfo.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterJwksUserInfoClientRegistration.class::isInstance);
    }

    @Test
    void testRetrievingRequestFilters_ClientRegistration() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.CLIENT_REGISTRATION);
        assertEquals(2, requestFilters.size());
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointClientRegistration.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterRegisteredClientClientRegistration.class::isInstance);
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.CLIENT_REGISTRATION, null);
        assertEquals(2, requestFilters.size());
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointClientRegistration.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterRegisteredClientClientRegistration.class::isInstance);
    }

    @Test
    void testRetrievingResponseFilters_ClientRegistration() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION);
        assertEquals(2, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointClientRegistration.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterJwksUserInfoClientRegistration.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.CLIENT_REGISTRATION, null);
        assertEquals(2, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointClientRegistration.class::isInstance);
        assertThat(responseFilters).anyMatch(ResponseFilterJwksUserInfoClientRegistration.class::isInstance);
    }

    @Test
    void testRetrievingRequestFilters_RegisteredClient() {
        var requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.REGISTERED_CLIENT);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointRegisteredClient.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterRegisteredClientClientRegistration.class::isInstance);
        assertEquals(2, requestFilters.size());
        requestFilters = filterStorage.getOidcRequestFilters(OidcEndpoint.Type.REGISTERED_CLIENT, null);
        assertThat(requestFilters).anyMatch(RequestFilterOidcEndpointRegisteredClient.class::isInstance);
        assertThat(requestFilters).anyMatch(RequestFilterRegisteredClientClientRegistration.class::isInstance);
        assertEquals(2, requestFilters.size());
    }

    @Test
    void testRetrievingResponseFilters_RegisteredClient() {
        var responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.REGISTERED_CLIENT);
        assertEquals(1, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointRegisteredClient.class::isInstance);
        responseFilters = filterStorage.getOidcResponseFilters(OidcEndpoint.Type.REGISTERED_CLIENT, null);
        assertEquals(1, responseFilters.size());
        assertThat(responseFilters).anyMatch(ResponseFilterOidcEndpointRegisteredClient.class::isInstance);
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.REGISTERED_CLIENT)
    public static class RequestFilterOidcEndpointRegisteredClient extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.REGISTERED_CLIENT)
    public static class ResponseFilterOidcEndpointRegisteredClient extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.CLIENT_REGISTRATION)
    public static class RequestFilterOidcEndpointClientRegistration extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.CLIENT_REGISTRATION)
    public static class ResponseFilterOidcEndpointClientRegistration extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.USERINFO)
    public static class RequestFilterOidcEndpointUserInfo extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.USERINFO)
    public static class ResponseFilterOidcEndpointUserInfo extends DummyOidcResponseFilter {
    }

    @Singleton
    @OidcEndpoint(OidcEndpoint.Type.JWKS)
    public static class RequestFilterOidcEndpointJWKS extends DummyOidcRequestFilter {
    }

    @ApplicationScoped
    @OidcEndpoint(OidcEndpoint.Type.JWKS)
    public static class ResponseFilterOidcEndpointJWKS extends DummyOidcResponseFilter {
    }

    @Dependent
    @OidcEndpoint({ OidcEndpoint.Type.JWKS, OidcEndpoint.Type.USERINFO, OidcEndpoint.Type.CLIENT_REGISTRATION })
    public static class ResponseFilterJwksUserInfoClientRegistration extends DummyOidcResponseFilter {
    }

    @Dependent
    @OidcEndpoint({ OidcEndpoint.Type.REGISTERED_CLIENT, OidcEndpoint.Type.CLIENT_REGISTRATION })
    public static class RequestFilterRegisteredClientClientRegistration extends DummyOidcRequestFilter {
    }

    @OnlyCodeFlowPrettyPlease
    @OidcEndpoint(OidcEndpoint.Type.REGISTERED_CLIENT)
    @Dependent
    public static class ConditionalRequestFilterWithOidcEndpoint extends DummyOidcRequestFilter {

    }

    @Target({ TYPE })
    @Retention(RUNTIME)
    public @interface OnlyCodeFlowPrettyPlease {

    }

    public static class OnlyCodeFlowPrettyPleasePredicate implements OidcFilterStorage.OidcRequestContextPredicate {

        @Override
        public boolean test(OidcRequestFilter.OidcRequestContext oidcRequestContext) {
            return "only-code-flow".equals(oidcRequestContext.requestBody().toString());
        }
    }

    public static class OnlyCodeBearerAuthPrettyPleasePredicate implements OidcFilterStorage.OidcResponseContextPredicate {

        @Override
        public boolean test(OidcResponseFilter.OidcResponseContext oidcResponseContext) {
            return "bearer".equals(oidcResponseContext.responseBody().toString());
        }
    }

    @Target({ TYPE })
    @Retention(RUNTIME)
    public @interface OnlyBearerAuthPrettyPlease {

    }

    public static class EndWithErPrettyPleasePredicate implements OidcFilterStorage.OidcResponseContextPredicate {

        @Override
        public boolean test(OidcResponseFilter.OidcResponseContext ctx) {
            return ctx.statusCode() == 200 && ctx.responseBody().toString().endsWith("er");
        }
    }

    @Target({ TYPE })
    @Retention(RUNTIME)
    public @interface EndWithErPrettyPlease {

    }

    @OnlyBearerAuthPrettyPlease
    @EndWithErPrettyPlease
    @Dependent
    @OidcEndpoint({ OidcEndpoint.Type.CLIENT_REGISTRATION, OidcEndpoint.Type.JWKS })
    public static class ResponseFilterWith2Conditions extends DummyOidcResponseFilter {
    }

    @OnlyCodeFlowPrettyPlease
    @OidcEndpoint(OidcEndpoint.Type.USERINFO)
    @Singleton
    public static class RequestFilterWithCondition1 extends DummyOidcRequestFilter {
    }

    @OnlyCodeFlowPrettyPlease
    @OidcEndpoint(OidcEndpoint.Type.USERINFO)
    @Singleton
    public static class RequestFilterWithCondition2 extends DummyOidcRequestFilter {
    }
}
