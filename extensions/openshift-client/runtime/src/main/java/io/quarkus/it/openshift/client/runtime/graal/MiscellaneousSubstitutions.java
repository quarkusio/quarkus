package io.quarkus.it.openshift.client.runtime.graal;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.miscellaneous.apiserver.v1.APIRequestCount;
import io.fabric8.openshift.api.model.miscellaneous.apiserver.v1.APIRequestCountList;
import io.fabric8.openshift.api.model.miscellaneous.cloudcredential.v1.CredentialsRequest;
import io.fabric8.openshift.api.model.miscellaneous.cloudcredential.v1.CredentialsRequestList;
import io.fabric8.openshift.api.model.miscellaneous.cncf.cni.v1.NetworkAttachmentDefinition;
import io.fabric8.openshift.api.model.miscellaneous.cncf.cni.v1.NetworkAttachmentDefinitionList;
import io.fabric8.openshift.api.model.miscellaneous.imageregistry.operator.v1.Config;
import io.fabric8.openshift.api.model.miscellaneous.imageregistry.operator.v1.ConfigList;
import io.fabric8.openshift.api.model.miscellaneous.metal3.v1alpha1.BareMetalHost;
import io.fabric8.openshift.api.model.miscellaneous.metal3.v1alpha1.BareMetalHostList;
import io.fabric8.openshift.api.model.miscellaneous.metal3.v1beta1.Metal3Remediation;
import io.fabric8.openshift.api.model.miscellaneous.metal3.v1beta1.Metal3RemediationList;
import io.fabric8.openshift.api.model.miscellaneous.metal3.v1beta1.Metal3RemediationTemplate;
import io.fabric8.openshift.api.model.miscellaneous.metal3.v1beta1.Metal3RemediationTemplateList;
import io.fabric8.openshift.api.model.miscellaneous.network.operator.v1.EgressRouter;
import io.fabric8.openshift.api.model.miscellaneous.network.operator.v1.EgressRouterList;
import io.fabric8.openshift.api.model.miscellaneous.network.operator.v1.OperatorPKI;
import io.fabric8.openshift.api.model.miscellaneous.network.operator.v1.OperatorPKIList;

/**
 * Allows the exclusion of the openshift-model-miscellaneous model without breaking the --link-at-build-time check.
 */
@TargetClass(className = "io.fabric8.openshift.client.impl.OpenShiftClientImpl", onlyWith = MiscellaneousSubstitutions.NoOpenShiftMiscellaneousModel.class)
public final class MiscellaneousSubstitutions {

    @Substitute
    public NonNamespaceOperation<APIRequestCount, APIRequestCountList, Resource<APIRequestCount>> apiRequestCounts() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<BareMetalHost, BareMetalHostList, Resource<BareMetalHost>> bareMetalHosts() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<CredentialsRequest, CredentialsRequestList, Resource<CredentialsRequest>> credentialsRequests() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<EgressRouter, EgressRouterList, Resource<EgressRouter>> egressRouters() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public NonNamespaceOperation<Config, ConfigList, Resource<Config>> imageRegistryOperatorConfigs() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<Metal3Remediation, Metal3RemediationList, Resource<Metal3Remediation>> metal3Remediations() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<Metal3RemediationTemplate, Metal3RemediationTemplateList, Resource<Metal3RemediationTemplate>> metal3RemediationTemplates() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<NetworkAttachmentDefinition, NetworkAttachmentDefinitionList, Resource<NetworkAttachmentDefinition>> networkAttachmentDefinitions() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<OperatorPKI, OperatorPKIList, Resource<OperatorPKI>> operatorPKIs() {
        throw new RuntimeException(Constants.ERROR_MESSAGE);
    }

    static final class Constants {
        private static final String ERROR_MESSAGE = "OpenShift Miscellaneous API is not available, please add the openshift-model-miscellaneous module to your classpath";
    }

    static final class NoOpenShiftMiscellaneousModel implements BooleanSupplier {

        private static final String OPENSHIFT_MODEL_MISCELLANEOUS_PACKAGE = "io.fabric8.openshift.api.model.miscellaneous.";
        static final Boolean OPENSHIFT_MODEL_MISCELLANEOUS_PRESENT = Arrays.stream(Package.getPackages())
                .map(Package::getName).anyMatch(p -> p.startsWith(OPENSHIFT_MODEL_MISCELLANEOUS_PACKAGE));

        @Override
        public boolean getAsBoolean() {
            return !OPENSHIFT_MODEL_MISCELLANEOUS_PRESENT;
        }
    }
}
