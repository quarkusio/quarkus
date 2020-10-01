package io.quarkus.liquibase.runtime.graal;

import java.security.SecureRandom;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "liquibase.util.StringUtil")
final class SubstituteStringUtil {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static SecureRandom rnd;

    @Substitute
    public static String randomIdentifer(int len) {
        SecureRandom rnd = new SecureRandom();
        final String AB = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++)
            sb.append(AB.charAt(rnd.nextInt(AB.length())));
        return sb.toString();
    }
}
