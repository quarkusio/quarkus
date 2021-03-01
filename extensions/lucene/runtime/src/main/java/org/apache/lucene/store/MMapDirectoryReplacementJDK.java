package org.apache.lucene.store;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.RecomputeFieldValue;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

@TargetClass(MMapDirectory.class)
final class MMapDirectoryReplacementJDK {

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias, isFinal = true)
    private static boolean UNMAP_SUPPORTED = false;

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias, isFinal = true)
    public static String UNMAP_NOT_SUPPORTED_REASON = "Not supported when running in GraalVM native mode";

    @Alias
    @RecomputeFieldValue(kind = RecomputeFieldValue.Kind.FromAlias, isFinal = true)
    private static ByteBufferGuard.BufferCleaner CLEANER = null;

    @Substitute
    private static Object unmapHackImpl() {
        return new Object();
    }

}