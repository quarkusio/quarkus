package io.quarkus.vertx.http.runtime.management;

import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
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

    /**
     * Initialize the management authentication handler with security policy and roles mapping.
     *
     * @param handler the authentication handler
     * @param beanContainer the bean container
     * @param managementConfig the management configuration
     */
    public static void initializeAuthenticationHandler(AuthenticationHandler handler, BeanContainer beanContainer,
            ManagementConfig managementConfig) {
        handler.init(beanContainer.beanInstance(ManagementPathMatchingHttpSecurityPolicy.class),
                RolesMapping.of(managementConfig.auth().rolesMapping()));
    }

    public static Handler<RoutingContext> permissionCheckHandler() {
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

}
