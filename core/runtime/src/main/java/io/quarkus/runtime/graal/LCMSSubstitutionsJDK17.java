package io.quarkus.runtime.graal;

import java.awt.color.ICC_Profile;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.util.JavaVersionUtil.JDK17OrLater;

@TargetClass(className = "sun.java2d.cmm.lcms.LCMS", onlyWith = JDK17OrLater.class)
final class Target_sun_java2d_cmm_lcms_LCMS_JDK17 {

    @Substitute
    static long loadProfileNative(byte[] data, Object ref) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    static void getProfileDataNative(long ptr) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    static byte[] getTagNative(long profileID, int signature) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    static void setTagDataNative(long ptr, int tagSignature,
            byte[] data) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public byte[] getProfileData(Profile p) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public byte[] getTagData(Profile p, int tagSignature) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public void setTagData(Profile p, int tagSignature, byte[] data) {
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
    static synchronized LCMSProfile getProfileID(ICC_Profile profile) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static void colorConvert(LCMSTransform trans,
            LCMSImageLayout src,
            LCMSImageLayout dest) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @TargetClass(className = "sun.java2d.cmm.Profile")
    static final class Profile {
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
