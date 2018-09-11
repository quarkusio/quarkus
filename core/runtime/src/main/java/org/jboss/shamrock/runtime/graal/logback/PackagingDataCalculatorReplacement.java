package org.jboss.shamrock.runtime.graal.logback;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import ch.qos.logback.classic.spi.StackTraceElementProxy;

@TargetClass(className = "ch.qos.logback.classic.spi.PackagingDataCalculator")
final class PackagingDataCalculatorReplacement {

    @Substitute
    String getCodeLocation(Class type) {
        return "na";
    }

    @Substitute
    void populateFrames(StackTraceElementProxy[] stepArray) {

    }


}
