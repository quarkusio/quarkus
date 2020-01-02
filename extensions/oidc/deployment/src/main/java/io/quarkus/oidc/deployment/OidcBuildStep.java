package io.quarkus.oidc.deployment;

import java.util.function.BooleanSupplier;

import org.eclipse.microprofile.jwt.Claim;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.EnableAllSecurityServicesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.oidc.runtime.BearerAuthenticationMechanism;
import io.quarkus.oidc.runtime.CodeAuthenticationMechanism;
import io.quarkus.oidc.runtime.DefaultTenantConfigResolver;
import io.quarkus.oidc.runtime.OidcBuildTimeConfig;
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.oidc.runtime.OidcJsonWebTokenProducer;
import io.quarkus.oidc.runtime.OidcRecorder;
import io.quarkus.oidc.runtime.OidcTokenCredentialProducer;
import io.quarkus.vertx.core.deployment.InternalWebVertxBuildItem;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;

@SuppressWarnings("deprecation")
public class OidcBuildStep {

    OidcBuildTimeConfig buildTimeConfig;

    @BuildStep(onlyIf = IsEnabled.class)
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.OIDC);
    }

    @BuildStep(onlyIf = IsEnabled.class)
    AdditionalBeanBuildItem jwtClaimIntegration(Capabilities capabilities) {
        if (!capabilities.isCapabilityPresent(Capabilities.JWT)) {
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
    public AdditionalBeanBuildItem beans() {
        AdditionalBeanBuildItem.Builder beans = AdditionalBeanBuildItem.builder().setUnremovable();

        if (OidcBuildTimeConfig.ApplicationType.SERVICE.equals(buildTimeConfig.applicationType)) {
            beans.addBeanClass(BearerAuthenticationMechanism.class);
        } else if (OidcBuildTimeConfig.ApplicationType.WEB_APP.equals(buildTimeConfig.applicationType)) {
            beans.addBeanClass(CodeAuthenticationMechanism.class);
        }
        return beans.addBeanClass(OidcJsonWebTokenProducer.class)
                .addBeanClass(OidcTokenCredentialProducer.class)
                .addBeanClass(OidcIdentityProvider.class)
                .addBeanClass(DefaultTenantConfigResolver.class).build();
    }

    @BuildStep(onlyIf = IsEnabled.class)
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(onlyIf = IsEnabled.class)
    public void setup(OidcConfig config, OidcRecorder recorder, InternalWebVertxBuildItem vertxBuildItem,
            BeanContainerBuildItem bc) {
        recorder.setup(config, vertxBuildItem.getVertx(), bc.getValue());
    }

    static class IsEnabled implements BooleanSupplier {
        OidcBuildTimeConfig config;

        public boolean getAsBoolean() {
            return config.enabled;
        }
    }
}
