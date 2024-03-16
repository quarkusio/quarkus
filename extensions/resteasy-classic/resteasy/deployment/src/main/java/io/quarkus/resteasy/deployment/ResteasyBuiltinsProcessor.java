package io.quarkus.resteasy.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.runtime.AuthenticationCompletionExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationFailedExceptionMapper;
import io.quarkus.resteasy.runtime.AuthenticationRedirectExceptionMapper;
import io.quarkus.resteasy.runtime.CompositeExceptionMapper;
import io.quarkus.resteasy.runtime.EagerSecurityFilter;
import io.quarkus.resteasy.runtime.ForbiddenExceptionMapper;
import io.quarkus.resteasy.runtime.JaxRsPermissionChecker;
import io.quarkus.resteasy.runtime.JaxRsSecurityConfig;
import io.quarkus.resteasy.runtime.SecurityContextFilter;
import io.quarkus.resteasy.runtime.StandardSecurityCheckInterceptor;
import io.quarkus.resteasy.runtime.UnauthorizedExceptionMapper;
import io.quarkus.resteasy.runtime.vertx.JsonArrayReader;
import io.quarkus.resteasy.runtime.vertx.JsonArrayWriter;
import io.quarkus.resteasy.runtime.vertx.JsonObjectReader;
import io.quarkus.resteasy.runtime.vertx.JsonObjectWriter;
import io.quarkus.security.spi.DefaultSecurityCheckBuildItem;

public class ResteasyBuiltinsProcessor {

    protected static final String META_INF_RESOURCES = "META-INF/resources";

    @BuildStep
    void setUpDenyAllJaxRs(JaxRsSecurityConfig securityConfig,
            BuildProducer<DefaultSecurityCheckBuildItem> defaultSecurityCheckProducer) {
        if (securityConfig.denyJaxRs) {
            defaultSecurityCheckProducer.produce(DefaultSecurityCheckBuildItem.denyAll());
        } else if (securityConfig.defaultRolesAllowed.isPresent()) {
            defaultSecurityCheckProducer
                    .produce(DefaultSecurityCheckBuildItem.rolesAllowed(securityConfig.defaultRolesAllowed.get()));
        }
    }

    /**
     * Install the JAX-RS security provider.
     */
    @BuildStep
    void setUpSecurity(BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
            BuildProducer<AdditionalBeanBuildItem> additionalBeanBuildItem, Capabilities capabilities) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(UnauthorizedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(ForbiddenExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationFailedExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationRedirectExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(AuthenticationCompletionExceptionMapper.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(CompositeExceptionMapper.class.getName()));
        if (capabilities.isPresent(Capability.SECURITY)) {
            providers.produce(new ResteasyJaxrsProviderBuildItem(SecurityContextFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(SecurityContextFilter.class));
            providers.produce(new ResteasyJaxrsProviderBuildItem(EagerSecurityFilter.class.getName()));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(EagerSecurityFilter.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem.unremovableOf(JaxRsPermissionChecker.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.RolesAllowedInterceptor.class));
            additionalBeanBuildItem.produce(AdditionalBeanBuildItem
                    .unremovableOf(StandardSecurityCheckInterceptor.PermissionsAllowedInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.PermitAllInterceptor.class));
            additionalBeanBuildItem.produce(
                    AdditionalBeanBuildItem.unremovableOf(StandardSecurityCheckInterceptor.AuthenticatedInterceptor.class));
        }
    }

    @BuildStep
    void vertxProviders(BuildProducer<ResteasyJaxrsProviderBuildItem> providers) {
        // These providers should work even if jackson-databind is not on the classpath
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonArrayWriter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectReader.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(JsonObjectWriter.class.getName()));
    }
}
