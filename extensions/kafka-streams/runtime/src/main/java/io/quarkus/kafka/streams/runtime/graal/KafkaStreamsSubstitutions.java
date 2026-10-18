package io.quarkus.kafka.streams.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * Resets the {@code initialized} field, so that the native libs are loaded again at
 * image runtime, after they have been loaded once at build time via calls from static
 * initializers.
 */
@TargetClass(className = "org.rocksdb.NativeLibraryLoader")
final class Target_org_rocksdb_NativeLibraryLoader {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.Reset)
    private static boolean initialized = false;
}

public final class KafkaStreamsSubstitutions {
}
