package io.quarkus.oidc.deployment;

import java.util.function.BooleanSupplier;

import jakarta.inject.Singleton;

import org.eclipse.microprofile.jwt.Claim;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SynthesisFinishedBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.BuildSteps;
import io.quarkus.deployment.annotations.Consume;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ExtensionSslNativeSupportBuildItem;
import io.quarkus.deployment.builditem.RuntimeConfigSetupCompleteBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.SecurityEvent;
import io.quarkus.oidc.TokenIntrospectionCache;
import io.quarkus.oidc.UserInfoCache;
import io.quarkus.oidc.runtime.BackChannelLogoutHandler;
import io.quarkus.oidc.runtime.DefaultTenantConfigResolver;
import io.quarkus.oidc.runtime.DefaultTokenIntrospectionUserInfoCache;
import io.quarkus.oidc.runtime.DefaultTokenStateManager;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcConfigurationMetadataProducer;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.oidc.runtime.OidcJsonWebTokenProducer;
import io.quarkus.oidc.runtime.OidcRecorder;
import io.quarkus.oidc.runtime.OidcSessionImpl;
import io.quarkus.oidc.runtime.OidcTokenCredentialProducer;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.quarkus.vertx.http.deployment.SecurityInformationBuildItem;
import io.smallrye.jwt.auth.cdi.ClaimValueProducer;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;

@BuildSteps(onlyIf = OidcBuildStep.IsEnabled.class)
public class OidcBuildStep {
    public static final DotName DOTNAME_SECURITY_EVENT = DotName.createSimple(SecurityEvent.class.getName());

    @BuildStep
    public void provideSecurityInformation(BuildProducer<SecurityInformationBuildItem> securityInformationProducer) {
        // TODO: By default quarkus.oidc.application-type = service
        // Also look at other options (web-app, hybrid)
        securityInformationProducer
                .produce(SecurityInformationBuildItem.OPENIDCONNECT("quarkus.oidc.auth-server-url"));
    }

    @BuildStep
    AdditionalBeanBuildItem jwtClaimIntegration(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.JWT)) {
            AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
            removable.addBeanClass(CommonJwtProducer.class);
            removable.addBeanClass(RawClaimTypeProducer.class);
            removable.addBeanClass(JsonValueProducer.class);
            removable.addBeanClass(ClaimValueProducer.class);
            removable.addBeanClass(Claim.class);
            return removable.build();
        }
        return null;
    }

    @BuildStep
    public void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(OidcAuthenticationMechanism.class)
                .addBeanClass(OidcJsonWebTokenProducer.class)
                .addBeanClass(OidcTokenCredentialProducer.class)
                .addBeanClass(OidcConfigurationMetadataProducer.class)
                .addBeanClass(OidcIdentityProvider.class)
                .addBeanClass(DefaultTenantConfigResolver.class)
                .addBeanClass(DefaultTokenStateManager.class)
                .addBeanClass(OidcSessionImpl.class)
                .addBeanClass(BackChannelLogoutHandler.class);
        additionalBeans.produce(builder.build());
    }

    @BuildStep(onlyIf = IsCacheEnabled.class)
    @Record(ExecutionTime.RUNTIME_INIT)
    public SyntheticBeanBuildItem addDefaultCacheBean(OidcConfig config,
            OidcRecorder recorder,
            CoreVertxBuildItem vertxBuildItem) {
        return SyntheticBeanBuildItem.configure(DefaultTokenIntrospectionUserInfoCache.class).unremovable()
                .types(DefaultTokenIntrospectionUserInfoCache.class, TokenIntrospectionCache.class, UserInfoCache.class)
                .supplier(recorder.setupTokenCache(config, vertxBuildItem.getVertx()))
                .scope(Singleton.class)
                .setRuntimeInit()
                .done();
    }

    @BuildStep
    ExtensionSslNativeSupportBuildItem enableSslInNative() {
        return new ExtensionSslNativeSupportBuildItem(Feature.OIDC);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public SyntheticBeanBuildItem setup(
            OidcConfig config,
            OidcRecorder recorder,
            CoreVertxBuildItem vertxBuildItem,
            TlsConfig tlsConfig) {
        return SyntheticBeanBuildItem.configure(TenantConfigBean.class).unremovable().types(TenantConfigBean.class)
                .supplier(recorder.setup(config, vertxBuildItem.getVertx(), tlsConfig))
                .destroyer(TenantConfigBean.Destroyer.class)
                .scope(Singleton.class) // this should have been @ApplicationScoped but fails for some reason
                .setRuntimeInit()
                .done();
    }

    // Note that DefaultTenantConfigResolver injects quarkus.http.proxy.enable-forwarded-prefix
    @Consume(RuntimeConfigSetupCompleteBuildItem.class)
    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    public void findSecurityEventObservers(
            OidcRecorder recorder,
            SynthesisFinishedBuildItem synthesisFinished) {
        boolean isSecurityEventObserved = synthesisFinished.getObservers().stream()
                .anyMatch(observer -> observer.asObserver().getObservedType().name().equals(DOTNAME_SECURITY_EVENT));
        recorder.setSecurityEventObserved(isSecurityEventObserved);
    }

    public static class IsEnabled implements BooleanSupplier {
        OidcBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }

    public static class IsCacheEnabled implements BooleanSupplier {
        OidcBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled && config.defaultTokenCacheEnabled;
        }
    }
}
