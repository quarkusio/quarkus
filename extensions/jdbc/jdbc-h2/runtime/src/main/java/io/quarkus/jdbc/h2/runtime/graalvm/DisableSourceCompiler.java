package io.quarkus.jdbc.h2.runtime.graalvm;

import java.lang.reflect.Method;

import javax.script.CompiledScript;
import javax.script.ScriptException;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@Substitute
@TargetClass(className = "org.h2.util.SourceCompiler")
public final class DisableSourceCompiler {

    private static final String ERR = "It's not possible to compile H2 triggers when embedding the engine in GraalVM native images";

    // Delete it all

    @Substitute
    public static boolean isJavaxScriptSource(String source) {
        throw new UnsupportedOperationException(ERR);
    }

    @Substitute
    public CompiledScript getCompiledScript(String packageAndClassName) throws ScriptException {
        throw new UnsupportedOperationException(ERR);
    }

    @Substitute
    public Class<?> getClass(String packageAndClassName) throws ClassNotFoundException {
        throw new UnsupportedOperationException(ERR);
    }

    @Substitute
    public void setSource(String className, String source) {
        // no-op
    }

    @Substitute
    public void setJavaSystemCompiler(boolean enabled) {
        // no-op
    }

    @Substitute
    public Method getMethod(String className) {
        throw new UnsupportedOperationException(ERR);
    }

}
