package io.quarkus.tika.runtime.graal;

@com.oracle.svm.core.annotate.Substitute
@com.oracle.svm.core.annotate.TargetClass(className = "org.apache.poi.poifs.nio.CleanerUtil")
public final class CleanerNotSupportedSubstitution {

    /**
     * <code>true</code>, if this platform supports unmapping mmapped files.
     */
    public static final boolean UNMAP_SUPPORTED = false;

    /**
     * if {@link #UNMAP_SUPPORTED} is {@code false}, this contains the reason
     * why unmapping is not supported.
     */
    public static final String UNMAP_NOT_SUPPORTED_REASON = "Not supported on GraalVM native-image";

    private static final org.apache.poi.poifs.nio.CleanerUtil.BufferCleaner CLEANER = null;

    /**
     * Reference to a BufferCleaner that does unmapping.
     * 
     * @return {@code null} if not supported.
     */
    public static org.apache.poi.poifs.nio.CleanerUtil.BufferCleaner getCleaner() {
        return CLEANER;
    }

}
