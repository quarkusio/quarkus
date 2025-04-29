package io.quarkus.jdbc.hsqldb.runtime.graalvm;

import org.h2.engine.Database;
import org.h2.util.SourceCompiler;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(Database.class)
public final class NoCompiledTriggersSupport {

    @Substitute
    public SourceCompiler getCompiler() {
        throw new UnsupportedOperationException(
                "It's not possible to compile H2 triggers when embedding the engine in GraalVM native images");
    }

}