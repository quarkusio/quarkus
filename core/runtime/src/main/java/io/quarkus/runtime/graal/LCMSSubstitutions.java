package io.quarkus.runtime.graal;

import java.awt.color.ICC_Profile;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.jdk.JDK16OrEarlier;

@TargetClass(className = "sun.java2d.cmm.lcms.LCMS", onlyWith = JDK16OrEarlier.class)
final class Target_sun_java2d_cmm_lcms_LCMS {

    @Substitute
    private long loadProfileNative(byte[] data, Object ref) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    private int getProfileSizeNative(long ptr) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    private void getProfileDataNative(long ptr, byte[] data) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    static byte[] getTagNative(long profileID, int signature) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    private void setTagDataNative(long ptr, int tagSignature,
            byte[] data) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    private static long createNativeTransform(
            long[] profileIDs, int renderType,
            int inFormatter, boolean isInIntPacked,
            int outFormatter, boolean isOutIntPacked,
            Object disposerRef) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static void initLCMS(Class<?> Trans, Class<?> IL, Class<?> Pf) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static synchronized LCMSProfile getProfileID(ICC_Profile profile) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static void colorConvert(LCMSTransform trans,
            LCMSImageLayout src,
            LCMSImageLayout dest) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @TargetClass(className = "sun.java2d.cmm.lcms.LCMSProfile")
    static final class LCMSProfile {
    }

    @TargetClass(className = "sun.java2d.cmm.lcms.LCMSImageLayout")
    static final class LCMSImageLayout {
    }

    @TargetClass(className = "sun.java2d.cmm.lcms.LCMSTransform")
    static final class LCMSTransform {
    }

}
