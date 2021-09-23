package io.quarkus.tika.runtime.graal;

import org.graalvm.nativeimage.ImageSingletons;
import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.impl.RuntimeClassInitializationSupport;

import com.oracle.svm.core.annotate.AutomaticFeature;

@AutomaticFeature
public class TikaFeature implements Feature {
    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
        final RuntimeClassInitializationSupport runtimeInit = ImageSingletons.lookup(RuntimeClassInitializationSupport.class);
        final String reason = "Quarkus run time init for Apache Tika";
        runtimeInit.initializeAtRunTime("org.apache.pdfbox", reason);
        runtimeInit.initializeAtRunTime("org.apache.poi.hssf.util", reason);
        runtimeInit.initializeAtRunTime("org.apache.poi.ss.format", reason);
    }
}
