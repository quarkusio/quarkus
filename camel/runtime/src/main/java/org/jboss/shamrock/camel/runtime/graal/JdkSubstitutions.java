package org.jboss.shamrock.camel.runtime.graal;

import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.ImageReader;
import javax.imageio.ImageTranscoder;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

class JdkSubstitutions {
}

//@TargetClass(className = "javax.xml.transform.TransformerException")
//final class Target_javax_xml_transform_TransformerException {
//    @Substitute
//    public void printStackTrace(java.io.PrintWriter s) {
//        s.print("Error omitted: javax.xml.transform.TransformerException is subtituted");
//    }
//}

@TargetClass(className = "javax.imageio.ImageIO")
final class Target_javax_imageio_ImageIO {

    @Substitute
    public static void scanForPlugins() {
    }

    @Substitute
    public static ImageInputStream createImageInputStream(Object input)
            throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static ImageOutputStream createImageOutputStream(Object output)
            throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }


    @Substitute
    public static String[] getReaderFormatNames() {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static String[] getReaderMIMETypes() {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static String[] getReaderFileSuffixes() {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageReader> getImageReaders(Object input) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageReader>
    getImageReadersByFormatName(String formatName) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageReader>
    getImageReadersBySuffix(String fileSuffix) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }


    @Substitute
    public static Iterator<ImageReader>
    getImageReadersByMIMEType(String MIMEType) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static String[] getWriterFormatNames() {

        throw new RuntimeException("Not Implemented yet on substrate");
    }

    /**
     * Returns an array of {@code String}s listing all of the
     * MIME types understood by the current set of registered
     * writers.
     *
     * @return an array of {@code String}s.
     */
    @Substitute
    public static String[] getWriterMIMETypes() {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    /**
     * Returns an array of {@code String}s listing all of the
     * file suffixes associated with the formats understood
     * by the current set of registered writers.
     *
     * @return an array of {@code String}s.
     * @since 1.6
     */
    @Substitute
    public static String[] getWriterFileSuffixes() {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageWriter>
    getImageWritersByFormatName(String formatName) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageWriter>
    getImageWritersBySuffix(String fileSuffix) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageWriter>
    getImageWritersByMIMEType(String MIMEType) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static ImageWriter getImageWriter(ImageReader reader) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static ImageReader getImageReader(ImageWriter writer) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageWriter>
    getImageWriters(ImageTypeSpecifier type, String formatName) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static Iterator<ImageTranscoder>
    getImageTranscoders(ImageReader reader, ImageWriter writer) {
        throw new RuntimeException("Not Implemented yet on substrate");
    }

    @Substitute
    public static BufferedImage read(File input) throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static BufferedImage read(InputStream input) throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static BufferedImage read(URL input) throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static BufferedImage read(ImageInputStream stream)
            throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static boolean write(RenderedImage im,
                                String formatName,
                                ImageOutputStream output) throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static boolean write(RenderedImage im,
                                String formatName,
                                File output) throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }

    @Substitute
    public static boolean write(RenderedImage im,
                                String formatName,
                                OutputStream output) throws IOException {
        throw new IOException("Not Implemented yet on substrate");
    }
}

@TargetClass(className = "sun.java2d.cmm.lcms.LCMS")
final class Target_sun_java2d_cmm_lcms_LCMS {

    @Substitute
    private long loadProfileNative(byte[] data, Object ref) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private int getProfileSizeNative(long ptr) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private void getProfileDataNative(long ptr, byte[] data) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    static byte[] getTagNative(long profileID, int signature) {
        throw new RuntimeException("Not Implemented");
    }


    /**
     * Writes supplied data as a tag into the profile.
     * Destroys old profile, if new one was successfully
     * created.
     * <p>
     * Returns valid pointer to new profile.
     * <p>
     * Throws CMMException if operation fails, preserve old profile from
     * destruction.
     */
    @Substitute
    private void setTagDataNative(long ptr, int tagSignature,
                                  byte[] data) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    private static long createNativeTransform(
            long[] profileIDs, int renderType,
            int inFormatter, boolean isInIntPacked,
            int outFormatter, boolean isOutIntPacked,
            Object disposerRef) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public static void initLCMS(Class<?> Trans, Class<?> IL, Class<?> Pf) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public static synchronized LCMSProfile getProfileID(ICC_Profile profile) {
        throw new RuntimeException("Not Implemented");
    }

    @Substitute
    public static void colorConvert(LCMSTransform trans,
                                    LCMSImageLayout src,
                                    LCMSImageLayout dest) {
        throw new RuntimeException("Not Implemented");
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
