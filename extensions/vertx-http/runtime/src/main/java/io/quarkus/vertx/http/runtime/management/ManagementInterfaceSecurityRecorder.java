package io.quarkus.vertx.http.runtime.management;

import java.util.Map;
import java.util.function.Supplier;

import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.CDI;

import io.quarkus.arc.runtime.BeanContainer;
import io.quarkus.arc.runtime.BeanContainerListener;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.vertx.http.runtime.security.AbstractPathMatchingHttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.BasicAuthenticationMechanism;
import io.quarkus.vertx.http.runtime.security.HttpSecurityPolicy;
import io.quarkus.vertx.http.runtime.security.HttpSecurityRecorder.AbstractAuthenticationHandler;
import io.quarkus.vertx.http.runtime.security.ManagementInterfaceHttpAuthorizer;
import io.quarkus.vertx.http.runtime.security.ManagementPathMatchingHttpSecurityPolicy;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

@Recorder
public class ManagementInterfaceSecurityRecorder {

    final RuntimeValue<ManagementInterfaceConfiguration> httpConfiguration;
    final ManagementInterfaceBuildTimeConfig buildTimeConfig;

    public ManagementInterfaceSecurityRecorder(RuntimeValue<ManagementInterfaceConfiguration> httpConfiguration,
            ManagementInterfaceBuildTimeConfig buildTimeConfig) {
        this.httpConfiguration = httpConfiguration;
        this.buildTimeConfig = buildTimeConfig;
    }

    public Handler<RoutingContext> authenticationMechanismHandler(boolean proactiveAuthentication) {
        return new ManagementAuthenticationHandler(proactiveAuthentication);
    }

    public Handler<RoutingContext> permissionCheckHandler(ManagementInterfaceBuildTimeConfig buildTimeConfig,
            Map<String, Supplier<HttpSecurityPolicy>> policies) {
        return new Handler<RoutingContext>() {
            volatile ManagementInterfaceHttpAuthorizer authorizer;

            @Override
            public void handle(RoutingContext event) {
                if (authorizer == null) {
                    if (authorizer == null) {
                        authorizer = CDI.current().select(ManagementInterfaceHttpAuthorizer.class).get();
                    }
                }
                authorizer.checkPermission(event);
            }
        };
    }

    public BeanContainerListener initPermissions(ManagementInterfaceBuildTimeConfig buildTimeConfig,
            Map<String, Supplier<HttpSecurityPolicy>> policies) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                container.beanInstance(ManagementPathMatchingHttpSecurityPolicy.class)
                        .init(buildTimeConfig.auth.permissions, policies, buildTimeConfig.rootPath);
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

    static class ManagementAuthenticationHandler extends AbstractAuthenticationHandler {

        volatile ManagementPathMatchingHttpSecurityPolicy pathMatchingPolicy;

        public ManagementAuthenticationHandler(boolean proactiveAuthentication) {
            super(proactiveAuthentication);
        }

        @Override
        protected void setPathMatchingPolicy(RoutingContext event) {
            if (pathMatchingPolicy == null) {
                Instance<ManagementPathMatchingHttpSecurityPolicy> pathMatchingPolicyInstance = CDI.current()
                        .select(ManagementPathMatchingHttpSecurityPolicy.class);
                pathMatchingPolicy = pathMatchingPolicyInstance.isResolvable() ? pathMatchingPolicyInstance.get() : null;
            }
            if (pathMatchingPolicy != null) {
                event.put(AbstractPathMatchingHttpSecurityPolicy.class.getName(), pathMatchingPolicy);
            }
        }
    }
}
