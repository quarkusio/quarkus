package io.quarkus.hibernate.orm.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.hibernate.orm.runtime.customized.QuarkusJtaPlatform;

/**
 * Technically this substitution shouldn't be needed, if only the DCO logic of the
 * native-image compiler was a bit smarter.
 * It doesn't seem to be able to detect that QuarkusJtaPlatformInitiator#jtaIsPresent
 * is a constant which will always be false when the JTA platform is not available.
 * (I had also tried to make this more obvious by adding a static final boolean,
 * but it has to "and" this boolean with the configuration flag and it doesn't seem
 * able to resolve that as a constant).
 */
@TargetClass(className = "io.quarkus.hibernate.orm.runtime.customized.QuarkusJtaPlatformInitiator", onlyWith = JtaIntegrationDCOHelper.Selector.class)
public final class JtaIntegrationDCOHelper {

    @Substitute
    private QuarkusJtaPlatform getJtaInstance() {
        throw new IllegalStateException(
                "This should never be called: JTA was enabled but apparently Narayana is not on the classpath?");
    }

    static final class Selector implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.arjuna.ats.jta.TransactionManager");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }

}
