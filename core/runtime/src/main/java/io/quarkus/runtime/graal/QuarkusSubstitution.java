package io.quarkus.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

@TargetClass(Quarkus.class)
final class QuarkusSubstitution {

    @Substitute
    private static void launchFromIDE(Class<? extends QuarkusApplication> quarkusApplication, String... args) {
        throw new RuntimeException("Should never be hit");
    }

}
