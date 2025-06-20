package io.quarkus.vertx.http.runtime.management;

import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.AuthenticationHandler;
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.ManagementPathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.RolesMapping;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ManagementSecurityRecorder {
    private final RuntimeValue<ManagementConfig> managementConfig;

    public ManagementSecurityRecorder(final RuntimeValue<ManagementConfig> managementConfig) {
        this.managementConfig = managementConfig;
    }

    public RuntimeValue<AuthenticationHandler> managementAuthenticationHandler(boolean proactiveAuthentication) {
        return new RuntimeValue<>(new AuthenticationHandler(proactiveAuthentication));
    }

    public Handler<RoutingContext> getAuthenticationHandler(RuntimeValue<AuthenticationHandler> handlerRuntimeValue) {
        return handlerRuntimeValue.getValue();
    }

    public void initializeAuthenticationHandler(RuntimeValue<AuthenticationHandler> handler, BeanContainer beanContainer) {
        handler.getValue().init(beanContainer.beanInstance(ManagementPathMatchingHttpSecurityPolicy.class),
                RolesMapping.of(managementConfig.getValue().auth().rolesMapping()));
    }

    public Handler<RoutingContext> permissionCheckHandler() {
        return new Handler<RoutingContext>() {
            private volatile ManagementInterfaceHttpAuthorizer authorizer;

            @Override
            public void handle(RoutingContext event) {
                if (authorizer == null) {
                    authorizer = CDI.current().select(ManagementInterfaceHttpAuthorizer.class).get();
                }
                authorizer.checkPermission(event);
            }
        };
    }

    public Supplier<?> setupBasicAuth() {
        return new Supplier<BasicAuthenticationMechanism>() {
            @Override
            public BasicAuthenticationMechanism get() {
                return new BasicAuthenticationMechanism(null, false);
            }
        };
    }
}
