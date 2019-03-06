package io.quarkus.runtime.graal;

import java.lang.invoke.MethodHandle;
import java.security.Principal;

import javax.security.auth.x500.X500Principal;

import org.wildfly.security.x500.util.X500PrincipalUtil;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 *
 */
@TargetClass(X500PrincipalUtil.class)
final class Target_org_wildfly_security_x500_util_X500PrincipalUtil {
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    @Alias
    static Class<?> X500_NAME_CLASS;
    @Delete
    static MethodHandle AS_X500_PRINCIPAL_HANDLE;

    @Substitute
    public static X500Principal asX500Principal(Principal principal, boolean convert) {
        return null;
    }
}
