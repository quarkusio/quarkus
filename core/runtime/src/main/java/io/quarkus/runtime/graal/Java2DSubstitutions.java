package io.quarkus.runtime.graal;

import java.awt.Graphics;
import java.awt.GraphicsEnvironment;

import com.oracle.svm.core.annotate.AlwaysInline;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(GraphicsEnvironment.class)
final class Target_java_awt_GraphicsEnvironment {
    @AlwaysInline("DCE for things using Java2D")
    @Substitute
    public static Graphics getLocalGraphicsEnvironment() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }
}

class Java2DSubstitutions {
}
