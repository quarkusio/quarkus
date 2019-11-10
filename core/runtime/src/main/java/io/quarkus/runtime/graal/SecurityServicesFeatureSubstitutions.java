package io.quarkus.runtime.graal;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.graalvm.nativeimage.hosted.Feature.BeforeAnalysisAccess;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.util.VMError;
import com.oracle.svm.hosted.SecurityServicesFeature;

/**
 * @deprecated This class contains a workaround for a NPE that is thrown when a third-party security provider (meaning not one
 *             from the JDK) is used in Quarkus. A GraalVM issue has been created for it, the fix should be part of GraalVM
 *             19.3.1. This class should be removed as soon as we integrate a GraalVM release that includes the fix.<br>
 *             See https://github.com/oracle/graal/issues/1883 for more details about the bug and the fix.
 */
@Deprecated
@TargetClass(value = SecurityServicesFeature.class, onlyWith = GraalVersion19_3_0.class)
final class Target_com_oracle_svm_hosted_SecurityServicesFeature {

    @Substitute
    private static Class<?> lambda$getConsParamClassAccessor$0(Map<String, Object> knownEngines, Field consParamClassNameField,
            BeforeAnalysisAccess access, java.lang.String serviceType) {
        try {
            /*
             * Access the Provider.knownEngines map and extract the EngineDescription
             * corresponding to the serviceType. Note that the map holds EngineDescription(s) of
             * only those service types that are shipped in the JDK. From the EngineDescription
             * object extract the value of the constructorParameterClassName field then, if the
             * class name is not null, get the corresponding Class<?> object and return it.
             */
            /* EngineDescription */Object engineDescription = knownEngines.get(serviceType);
            /*
             * This isn't an engine known to the Provider (which actually means that it isn't
             * one that's shipped in the JDK), so we don't have the predetermined knowledge of
             * the constructor param class.
             */
            if (engineDescription == null) {
                return null;
            }
            String constrParamClassName = (String) consParamClassNameField.get(engineDescription);
            if (constrParamClassName != null) {
                return access.findClassByName(constrParamClassName);
            }
        } catch (IllegalAccessException e) {
            VMError.shouldNotReachHere(e);
        }
        return null;

    }
}

final class GraalVersion19_3_0 implements BooleanSupplier {
    public boolean getAsBoolean() {
        final String version = System.getProperty("org.graalvm.version");
        return version.startsWith("19.3.");
    }
}
