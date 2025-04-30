package io.quarkus.it.openshift.client.runtime.graal;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operator.imageregistry.v1.Config;
import io.fabric8.openshift.api.model.operator.imageregistry.v1.ConfigList;
import io.fabric8.openshift.api.model.operator.network.v1.EgressRouter;
import io.fabric8.openshift.api.model.operator.network.v1.EgressRouterList;
import io.fabric8.openshift.api.model.operator.network.v1.OperatorPKI;
import io.fabric8.openshift.api.model.operator.network.v1.OperatorPKIList;
import io.fabric8.openshift.client.dsl.OpenShiftOperatorAPIGroupDSL;

/**
 * Allows the exclusion of the openshift-model-operator model without breaking the --link-at-build-time check.
 */
@TargetClass(className = "io.fabric8.openshift.client.impl.OpenShiftClientImpl", onlyWith = OperatorSubstitutions.NoOpenShiftOperatorModel.class)
public final class OperatorSubstitutions {

    @Substitute
    public MixedOperation<EgressRouter, EgressRouterList, Resource<EgressRouter>> egressRouters() {
        throw new RuntimeException(OperatorSubstitutions.Constants.ERROR_MESSAGE);
    }

    @Substitute
    public NonNamespaceOperation<Config, ConfigList, Resource<Config>> imageRegistryOperatorConfigs() {
        throw new RuntimeException(OperatorSubstitutions.Constants.ERROR_MESSAGE);
    }

    @Substitute
    public OpenShiftOperatorAPIGroupDSL operator() {
        throw new RuntimeException(OperatorSubstitutions.Constants.ERROR_MESSAGE);
    }

    @Substitute
    public MixedOperation<OperatorPKI, OperatorPKIList, Resource<OperatorPKI>> operatorPKIs() {
        throw new RuntimeException(OperatorSubstitutions.Constants.ERROR_MESSAGE);
    }

    static final class Constants {
        private static final String ERROR_MESSAGE = "OpenShift Operator API is not available, please add the openshift-model-operator module to your classpath";
    }

    static final class NoOpenShiftOperatorModel implements BooleanSupplier {

        private static final String OPENSHIFT_MODEL_OPERATOR_PACKAGE = "io.fabric8.openshift.api.model.operator.";
        static final Boolean OPENSHIFT_MODEL_OPERATOR_PRESENT = Arrays.stream(Package.getPackages())
                .map(Package::getName).anyMatch(p -> p.startsWith(OPENSHIFT_MODEL_OPERATOR_PACKAGE));

        @Override
        public boolean getAsBoolean() {
            return !OPENSHIFT_MODEL_OPERATOR_PRESENT;
        }
    }
}
