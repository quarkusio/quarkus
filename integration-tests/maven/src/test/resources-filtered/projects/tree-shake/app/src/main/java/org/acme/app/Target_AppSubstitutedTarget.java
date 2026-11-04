package org.acme.app;

import org.acme.substitution.AppSubstitutionHelper;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.acme.substitution.AppSubstitutedTarget")
public final class Target_AppSubstitutedTarget {

    @Substitute
    public String hello() {
        return AppSubstitutionHelper.hello();
    }
}
