package org.acme.substitution;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.acme.substitution.UnusedTarget")
public final class Target_UnusedTarget {

    @Substitute
    public String hello() {
        return "substituted UnusedTarget";
    }
}
