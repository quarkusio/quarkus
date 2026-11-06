package org.acme.substitution;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.acme.substitution.UsedTarget")
public final class Target_UsedTarget {

    @Substitute
    public String hello() {
        return "substituted UsedTarget";
    }
}
