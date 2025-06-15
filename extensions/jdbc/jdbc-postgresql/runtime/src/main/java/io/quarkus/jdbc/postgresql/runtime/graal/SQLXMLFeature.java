package io.quarkus.jdbc.postgresql.runtime.graal;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;

public final class SQLXMLFeature implements Feature {

    private final AtomicBoolean triggered = new AtomicBoolean(false);

    /**
     * To set this, add `-J-Dio.quarkus.jdbc.postgresql.graalvm.diagnostics=true` to the native-image parameters
     */
    private static final boolean log = Boolean.getBoolean("io.quarkus.jdbc.postgresql.graalvm.diagnostics");

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        ImageSingletons.add(DomHelper.class, new DomHelper());
    }

    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        Class<?> pgSQLXMLClass = access.findClassByName("io.quarkus.jdbc.postgresql.runtime.graal.PgSQLXML");
        try {
            // Only if this method is ever invoked, then it's possible that field {@link PgSQLXML#domResult }
            // is ever non-null, and only in that case the XML parsing facilities pulled in by the driver
            // end up being necessary.
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
                        "Quarkus' automatic feature for GraalVM native images: enabling support for XML processing in the PostgreSQL JDBC driver");
            }
            DomHelper.enableXmlProcessing();
        }
    }

}
