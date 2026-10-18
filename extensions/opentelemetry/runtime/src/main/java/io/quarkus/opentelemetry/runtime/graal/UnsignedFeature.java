package io.quarkus.opentelemetry.runtime.graal;

import org.graalvm.nativeimage.hosted.Feature;
import org.graalvm.nativeimage.hosted.RuntimeProxyCreation;

import jdk.jfr.Unsigned;

public class UnsignedFeature implements Feature {
    @Override
    public void beforeAnalysis(BeforeAnalysisAccess access) {
        /* Needed for RecordedObject.getLong(). */
        RuntimeProxyCreation.register(Unsigned.class);
    }
}
