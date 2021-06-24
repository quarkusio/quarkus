package io.quarkus.jdbc.postgresql.runtime.graal;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.dom.DOMResult;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeReflection;
import org.postgresql.core.BaseConnection;

import com.oracle.svm.core.annotate.AutomaticFeature;

@AutomaticFeature
public final class SQLXLMFeature implements Feature {

    private final AtomicBoolean triggered = new AtomicBoolean(false);

    /**
     * To set this, add `-J-Dio.quarkus.jdbc.postgresql.graalvm.diagnostics=true` to the native-image parameters
     */
    private static final boolean log = Boolean.getBoolean("io.quarkus.jdbc.postgresql.graalvm.diagnostics");

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> pgSQLXMLClass = access.findClassByName("io.quarkus.jdbc.postgresql.runtime.graal.PgSQLXML");
        try {
            //Only if this method is ever invoked, then it's possible that field {@link PgSQLXML#domResult }
            //is ever non-null, and only in that case the XML parsing facilities pulled in by the driver
            //end up being necessary.
            final Method triggerMethod = pgSQLXMLClass.getMethod("setResult", Class.class);
            access.registerReachabilityHandler(this::identifiedXMLProcessingInDriver, triggerMethod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void identifiedXMLProcessingInDriver(DuringAnalysisAccess duringAnalysisAccess) {
        final boolean needsEnablingYet = triggered.compareAndSet(false, true);
        if (needsEnablingYet) {
            if (log) {
                System.out.println(
                        "Quarkus's automatic feature for GraalVM native images: enabling support for XML processing in the PosgreSQL JDBC driver");
            }
            enableDomXMLProcessingInDriver(duringAnalysisAccess);
        }
    }

    private void enableDomXMLProcessingInDriver(DuringAnalysisAccess duringAnalysisAccess) {
        final Class<?> classByName = duringAnalysisAccess.findClassByName("io.quarkus.jdbc.postgresql.runtime.graal.DomHelper");
        try {
            final Method method = classByName.getMethod("reallyProcessDomResult", DOMResult.class, BaseConnection.class);
            RuntimeReflection.register(method);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

}
