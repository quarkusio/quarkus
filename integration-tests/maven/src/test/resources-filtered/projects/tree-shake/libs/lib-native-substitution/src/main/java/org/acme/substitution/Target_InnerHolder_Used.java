package org.acme.substitution;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(className = "org.acme.substitution.InnerHolder", innerClass = "Used")
public final class Target_InnerHolder_Used {

    @Substitute
    public String hello() {
        return "substituted InnerHolder.Used";
    }
}
