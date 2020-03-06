package io.quarkus.liquibase.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The liquibase license service for the commercial liquibase license
 * throws UnsupportedFeatureException: Non-reducible loop for the liquibase.pro.packaged.aO.parseNumberText2(boolean):
 *
 * With this substitute the liquibase license service will be disabled.
 */
@TargetClass(className = "liquibase.pro.packaged.kp")
final class SubstituteKp {

    @Substitute
    public boolean licenseIsValid(String license) {
        return false;
    }
}
