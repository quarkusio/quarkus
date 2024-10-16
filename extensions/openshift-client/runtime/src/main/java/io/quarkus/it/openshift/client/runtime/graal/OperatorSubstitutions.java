package io.quarkus.it.openshift.client.runtime.graal;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.fabric8.openshift.client.dsl.OpenShiftOperatorAPIGroupDSL;

/**
 * Allows the exclusion of the openshift-model-operator model without breaking the --link-at-build-time check.
 */
@TargetClass(className = "io.fabric8.openshift.client.impl.OpenShiftClientImpl", onlyWith = OperatorSubstitutions.NoOpenShiftOperatorModel.class)
public final class OperatorSubstitutions {

    @Substitute
    public OpenShiftOperatorAPIGroupDSL operator() {
        throw new RuntimeException(
                "OpenShift Operator API is not available, please add the openshift-model-operator module to your classpath");
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
