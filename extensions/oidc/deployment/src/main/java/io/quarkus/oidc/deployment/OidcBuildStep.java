package io.quarkus.oidc.deployment;

import java.util.function.BooleanSupplier;

import javax.inject.Singleton;

import org.eclipse.microprofile.jwt.Claim;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.oidc.runtime.DefaultTenantConfigResolver;
import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.oidc.runtime.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.oidc.runtime.OidcJsonWebTokenProducer;
import io.quarkus.oidc.runtime.OidcRecorder;
import io.quarkus.oidc.runtime.OidcTokenCredentialProducer;
import io.quarkus.oidc.runtime.TenantConfigBean;
import io.quarkus.vertx.core.deployment.CoreVertxBuildItem;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;
import io.smallrye.jwt.build.impl.JwtProviderImpl;

public class OidcBuildStep {

    OidcBuildTimeConfig buildTimeConfig;

    @BuildStep(onlyIf = IsEnabled.class)
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(Feature.OIDC);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    AdditionalBeanBuildItem jwtClaimIntegration(Capabilities capabilities) {
        if (!capabilities.isPresent(Capability.JWT)) {
            AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
            removable.addBeanClass(CommonJwtProducer.class);
            removable.addBeanClass(RawClaimTypeProducer.class);
            removable.addBeanClass(JsonValueProducer.class);
            removable.addBeanClass(Claim.class);
            return removable.build();
        }
        return null;
    }

    @BuildStep(onlyIf = IsEnabled.class)
    public void additionalBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder().setUnremovable();

        builder.addBeanClass(OidcAuthenticationMechanism.class)
                .addBeanClass(OidcJsonWebTokenProducer.class)
                .addBeanClass(OidcTokenCredentialProducer.class)
                .addBeanClass(OidcIdentityProvider.class)
                .addBeanClass(DefaultTenantConfigResolver.class);
        additionalBeans.produce(builder.build());

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, JwtProviderImpl.class));
    }

    @BuildStep(onlyIf = IsEnabled.class)
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsEnabled.class)
    public SyntheticBeanBuildItem setup(
            OidcConfig config,
            OidcRecorder recorder,
            CoreVertxBuildItem vertxBuildItem) {
        return SyntheticBeanBuildItem.configure(TenantConfigBean.class).unremovable().types(TenantConfigBean.class)
                .supplier(recorder.setup(config, vertxBuildItem.getVertx()))
                .scope(Singleton.class)
                .setRuntimeInit()
                .done();
    }

    static class IsEnabled implements BooleanSupplier {
        OidcBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
