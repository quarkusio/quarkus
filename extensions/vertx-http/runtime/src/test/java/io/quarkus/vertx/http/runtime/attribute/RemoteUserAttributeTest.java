package io.quarkus.vertx.http.runtime.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.QuarkusHttpUser;
import io.vertx.ext.web.RoutingContext;

@ExtendWith(MockitoExtension.class)
class RemoteUserAttributeTest {

    @Mock
    RoutingContext routingContext;

    @Mock
    SecurityIdentity securityIdentity;

    @Test
    void shouldReturnNullWhenPrincipalIsNull() {
        when(securityIdentity.getPrincipal()).thenReturn(null);
        when(routingContext.user()).thenReturn(new QuarkusHttpUser(securityIdentity));

        assertThat(RemoteUserAttribute.INSTANCE.readAttribute(routingContext)).isNull();
    }

    @Test
    void shouldReturnUserNameWhenPrincipalIsNotNull() {
        when(securityIdentity.getPrincipal()).thenReturn(() -> "alice");
        when(routingContext.user()).thenReturn(new QuarkusHttpUser(securityIdentity));

        assertThat(RemoteUserAttribute.INSTANCE.readAttribute(routingContext)).isEqualTo("alice");
    }
}
