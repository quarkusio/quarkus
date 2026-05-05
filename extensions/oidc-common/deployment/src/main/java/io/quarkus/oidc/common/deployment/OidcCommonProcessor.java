package io.quarkus.oidc.common.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.oidc.common.runtime.CertificateUpdateEventListener;

public class OidcCommonProcessor {

    @BuildStep
    AdditionalBeanBuildItem registerCertificateUpdateEventListener() {
        return AdditionalBeanBuildItem.unremovableOf(CertificateUpdateEventListener.class);
    }

}
