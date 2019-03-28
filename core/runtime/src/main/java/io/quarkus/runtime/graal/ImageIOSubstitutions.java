package io.quarkus.runtime.graal;

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

@TargetClass(className = "javax.imageio.ImageIO")
final class Target_javax_imageio_ImageIO {

    @Substitute
    public static void scanForPlugins() {
    }

    @Substitute
    public static ImageInputStream createImageInputStream(Object input)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static ImageOutputStream createImageOutputStream(Object output)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static String[] getReaderFormatNames() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static String[] getReaderMIMETypes() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static String[] getReaderFileSuffixes() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageReader> getImageReaders(Object input) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageReader> getImageReadersByFormatName(String formatName) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageReader> getImageReadersBySuffix(String fileSuffix) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageReader> getImageReadersByMIMEType(String MIMEType) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static String[] getWriterFormatNames() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static String[] getWriterMIMETypes() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static String[] getWriterFileSuffixes() {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageWriter> getImageWritersByFormatName(String formatName) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageWriter> getImageWritersBySuffix(String fileSuffix) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageWriter> getImageWritersByMIMEType(String MIMEType) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static ImageWriter getImageWriter(ImageReader reader) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static ImageReader getImageReader(ImageWriter writer) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageWriter> getImageWriters(ImageTypeSpecifier type, String formatName) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static Iterator<ImageTranscoder> getImageTranscoders(ImageReader reader, ImageWriter writer) {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static BufferedImage read(File input) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static BufferedImage read(InputStream input) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static BufferedImage read(URL input) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static BufferedImage read(ImageInputStream stream)
            throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static boolean write(RenderedImage im,
            String formatName,
            ImageOutputStream output) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static boolean write(RenderedImage im,
            String formatName,
            File output) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }

    @Substitute
    public static boolean write(RenderedImage im,
            String formatName,
            OutputStream output) throws IOException {
        throw new UnsupportedOperationException("Not implemented yet for GraalVM native images");
    }
}
