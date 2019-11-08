package io.quarkus.oidc.deployment;

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
import io.quarkus.oidc.runtime.OidcConfig;
import io.quarkus.oidc.runtime.OidcIdentityProvider;
import io.quarkus.oidc.runtime.OidcJsonWebTokenProducer;
import io.quarkus.oidc.runtime.OidcRecorder;
import io.quarkus.oidc.runtime.OidcTokenCredentialProducer;
import io.quarkus.vertx.deployment.VertxBuildItem;
import io.smallrye.jwt.auth.cdi.CommonJwtProducer;
import io.smallrye.jwt.auth.cdi.JsonValueProducer;
import io.smallrye.jwt.auth.cdi.RawClaimTypeProducer;

public class OidcBuildStep {

    @BuildStep
    FeatureBuildItem featureBuildItem() {
        return new FeatureBuildItem(FeatureBuildItem.OIDC);
    }

    @BuildStep
    AdditionalBeanBuildItem jwtClaimIntegration(Capabilities capabilities, OidcConfig config) {
        if (!capabilities.isCapabilityPresent(Capabilities.JWT) && config.enabled) {
            AdditionalBeanBuildItem.Builder removable = AdditionalBeanBuildItem.builder();
            removable.addBeanClass(CommonJwtProducer.class);
            removable.addBeanClass(RawClaimTypeProducer.class);
            removable.addBeanClass(JsonValueProducer.class);
            removable.addBeanClass(Claim.class);
            return removable.build();
        }
        return null;
    }

    @BuildStep
    public AdditionalBeanBuildItem beans(OidcConfig config) {
        if (config.enabled) {
            AdditionalBeanBuildItem.Builder beans = AdditionalBeanBuildItem.builder().setUnremovable();

            if (OidcConfig.ApplicationType.SERVICE.equals(config.getApplicationType())) {
                beans.addBeanClass(BearerAuthenticationMechanism.class);
            } else if (OidcConfig.ApplicationType.WEB_APP.equals(config.getApplicationType())) {
                beans.addBeanClass(CodeAuthenticationMechanism.class);
            }
            return beans.addBeanClass(OidcJsonWebTokenProducer.class)
                    .addBeanClass(OidcTokenCredentialProducer.class)
                    .addBeanClass(OidcIdentityProvider.class).build();
        }

        return null;
    }

    @BuildStep
    EnableAllSecurityServicesBuildItem security() {
        return new EnableAllSecurityServicesBuildItem();
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    public void setup(OidcConfig config, OidcRecorder recorder, VertxBuildItem vertxBuildItem,
            BeanContainerBuildItem bc) {
        if (config.enabled) {
            recorder.setup(config, vertxBuildItem.getVertx(), bc.getValue());
        }
    }
}
