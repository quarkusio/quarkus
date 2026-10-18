package io.quarkus.jdbc.mariadb.runtime.graal;

import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * The MariaDB driver's {@code AwsIamCredentialPlugin} references
 * {@code software.amazon.awssdk.services.rds.RdsUtilities}, which is only
 * available when the RDS SDK is on the classpath. When it is absent, GraalVM's
 * analysis fails with {@code ClassNotFoundException}.
 * <p>
 * The driver's own {@code reflect-config.json} guards this class behind a
 * {@code typeReachable} condition on {@code AwsBasicCredentials}, but that
 * condition is too broad: any AWS SDK module satisfies it.
 * <p>
 * We delete the plugin (and its helper class) from the native image when the
 * RDS SDK is not available, so GraalVM never attempts to analyze them.
 *
 * @see <a href="https://jira.mariadb.org/browse/CONJ-1360">CONJ-1360</a>
 * @see <a href="https://github.com/quarkusio/quarkus/issues/56834">quarkusio/quarkus#56834</a>
 */
@Delete
@TargetClass(className = "org.mariadb.jdbc.plugin.credential.aws.AwsIamCredentialPlugin", onlyWith = RdsUnavailable.class)
final class Delete_AwsIamCredentialPlugin {
}

@Delete
@TargetClass(className = "org.mariadb.jdbc.plugin.credential.aws.AwsCredentialGenerator", onlyWith = RdsUnavailable.class)
final class Delete_AwsCredentialGenerator {
}

final class RdsUnavailable implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            Class.forName("software.amazon.awssdk.services.rds.RdsUtilities");
            return false;
        } catch (ClassNotFoundException e) {
            return true;
        }
    }
}
