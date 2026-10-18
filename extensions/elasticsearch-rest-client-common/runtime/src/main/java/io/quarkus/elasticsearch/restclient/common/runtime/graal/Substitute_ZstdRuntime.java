package io.quarkus.elasticsearch.restclient.common.runtime.graal;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

// This is an optional compressing capability of the Apache 5 client.
// It checks for the optional dependency (com.github.luben.zstd.Zstd)
// and since we do not support that, we might as well make GraalVM happy
// and just drop the capability by making it always return false.
@TargetClass(className = "org.apache.hc.client5.http.impl.ZstdRuntime")
final class Substitute_ZstdRuntime {

    @Substitute
    public static boolean available() {
        return false;
    }
}
