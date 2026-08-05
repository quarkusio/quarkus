package io.quarkus.oidc.client.reactive.filter;

import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.oidc.client.reactive.filter.runtime.AbstractOidcClientRequestReactiveFilter;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.keycloak.server.KeycloakTestResourceLifecycleManager;

@QuarkusTestResource(KeycloakTestResourceLifecycleManager.class)
public class OidcClientRequestFilterRevokedTokenDevModeTest extends AbstractRevokedAccessTokenDevModeTest {

    @RegisterExtension
    static final QuarkusDevModeTest test = createQuarkusDevModeTest(
            """
                    %s/mp-rest/url=http://localhost:${quarkus.http.port}
                    %s/mp-rest/url=http://localhost:${quarkus.http.port}
                    """.formatted(MyDefaultClientMultipleProviders.class.getName(),
                    MyNamedClientMultipleProviders.class.getName()),
            MyDefaultClient.class, MyNamedClient.class,
            MyNamedClientWithoutRefresh.class, MyDefaultClientWithoutRefresh.class, MyClientResourceImpl.class,
            DefaultClientRefreshEnabled.class, NamedClientRefreshEnabled.class, DefaultClientRefreshDisabled.class,
            NamedClientRefreshDisabled.class, MyClientExceptionMapper.class, MyDefaultClientMultipleProviders.class,
            MyNamedClientMultipleProviders.class);

    @Test
    void verifyDefaultClientHasTokenRefreshedOn401() {
        verifyTokenRefreshedOn401(MyClientCategory.DEFAULT_CLIENT);
        verifyTokenRefreshedOn401(MyClientCategory.DEFAULT_CLIENT_MULTIPLE_PROVIDERS);
    }

    @Test
    void verifyNamedClientHasTokenRefreshedOn401() {
        verifyTokenRefreshedOn401(MyClientCategory.NAMED_CLIENT);
        verifyTokenRefreshedOn401(MyClientCategory.NAMED_CLIENT_MULTIPLE_PROVIDERS);
    }

    @RegisterRestClient
    @RegisterProvider(value = DefaultClientRefreshEnabled.class)
    @Path(MY_SERVER_RESOURCE_PATH)
    public interface MyDefaultClient extends MyClient {

    }

    @Priority(Priorities.AUTHENTICATION)
    public static class DefaultClientRefreshEnabled extends AbstractOidcClientRequestReactiveFilter {
        @Override
        protected boolean refreshOnUnauthorized() {
            return true;
        }
    }

    @RegisterRestClient
    @RegisterProvider(value = NamedClientRefreshEnabled.class)
    @Path(MY_SERVER_RESOURCE_PATH)
    public interface MyNamedClient extends MyClient {

    }

    @Priority(Priorities.AUTHENTICATION)
    public static class NamedClientRefreshEnabled extends AbstractOidcClientRequestReactiveFilter {
        @Override
        protected boolean refreshOnUnauthorized() {
            return true;
        }

        @Override
        protected Optional<String> clientId() {
            return Optional.of(NAMED_CLIENT);
        }
    }

    @RegisterRestClient
    @RegisterProvider(value = DefaultClientRefreshDisabled.class)
    @Path(MY_SERVER_RESOURCE_PATH)
    public interface MyDefaultClientWithoutRefresh extends MyClient {

    }

    @Priority(Priorities.AUTHENTICATION)
    public static class DefaultClientRefreshDisabled extends AbstractOidcClientRequestReactiveFilter {
    }

    @RegisterRestClient
    @RegisterProvider(value = NamedClientRefreshDisabled.class)
    @Path(MY_SERVER_RESOURCE_PATH)
    public interface MyNamedClientWithoutRefresh extends MyClient {

    }

    @Priority(Priorities.AUTHENTICATION)
    public static class NamedClientRefreshDisabled extends AbstractOidcClientRequestReactiveFilter {
        @Override
        protected Optional<String> clientId() {
            return Optional.of(NAMED_CLIENT);
        }
    }

    @RegisterRestClient
    @RegisterProvider(value = DefaultClientRefreshEnabled.class)
    @RegisterProvider(value = MyClientExceptionMapper.class)
    @Path(MY_SERVER_RESOURCE_PATH)
    public interface MyDefaultClientMultipleProviders extends MyClient {
    }

    @RegisterRestClient
    @RegisterProvider(value = NamedClientRefreshEnabled.class)
    @RegisterProvider(value = MyClientExceptionMapper.class)
    @Path(MY_SERVER_RESOURCE_PATH)
    public interface MyNamedClientMultipleProviders extends MyClient {
    }

    public static class MyClientExceptionMapper implements ResponseExceptionMapper<RuntimeException> {
        @Override
        public boolean handles(int status, MultivaluedMap<String, Object> headers) {
            return status == 500;
        }

        @Override
        public RuntimeException toThrowable(Response response) {
            return new RuntimeException("Error");
        }
    }

    @Path(MY_CLIENT_RESOURCE_PATH)
    public static class MyClientResourceImpl extends MyClientResource {

        private final MyDefaultClient myDefaultClient;
        private final MyNamedClient myNamedClient;
        private final MyDefaultClientWithoutRefresh myDefaultClientWithoutRefresh;
        private final MyNamedClientWithoutRefresh myNamedClientWithoutRefresh;
        private final MyDefaultClientMultipleProviders myDefaultClientMultipleProviders;
        private final MyNamedClientMultipleProviders myNamedClientMultipleProviders;

        public MyClientResourceImpl(@RestClient MyDefaultClient myDefaultClient, @RestClient MyNamedClient myNamedClient,
                @RestClient MyDefaultClientWithoutRefresh myDefaultClientWithoutRefresh,
                @RestClient MyNamedClientWithoutRefresh myNamedClientWithoutRefresh,
                @RestClient MyDefaultClientMultipleProviders myDefaultClientMultipleProviders,
                @RestClient MyNamedClientMultipleProviders myNamedClientMultipleProviders) {
            this.myDefaultClient = myDefaultClient;
            this.myNamedClient = myNamedClient;
            this.myDefaultClientWithoutRefresh = myDefaultClientWithoutRefresh;
            this.myNamedClientWithoutRefresh = myNamedClientWithoutRefresh;
            this.myDefaultClientMultipleProviders = myDefaultClientMultipleProviders;
            this.myNamedClientMultipleProviders = myNamedClientMultipleProviders;
        }

        @Override
        protected MyClient myDefaultClient() {
            return myDefaultClient;
        }

        @Override
        protected MyClient myNamedClient() {
            return myNamedClient;
        }

        @Override
        protected MyClient myDefaultClientWithoutRefresh() {
            return myDefaultClientWithoutRefresh;
        }

        @Override
        protected MyClient myNamedClientWithoutRefresh() {
            return myNamedClientWithoutRefresh;
        }

        @Override
        protected MyClient myDefaultClientMultipleProviders() {
            return myDefaultClientMultipleProviders;
        }

        @Override
        protected MyClient myNamedClientMultipleProviders() {
            return myNamedClientMultipleProviders;
        }
    }

}
