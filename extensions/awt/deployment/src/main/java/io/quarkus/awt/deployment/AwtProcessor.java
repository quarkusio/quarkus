package io.quarkus.awt.deployment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;
import org.jboss.logging.Logger;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Produce;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.pkg.NativeConfig;
import io.quarkus.deployment.pkg.builditem.ArtifactResultBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;

class AwtProcessor {

    private static final Logger LOGGER = Logger.getLogger(AwtProcessor.class.getName());

    public static final Pattern JDK_VERSION_FROM_NATIVE_IMAGE = Pattern
            .compile(".*(Java Version ([0-9]*)\\.([0-9]*)\\.([0-9]*).*)\\).*");

    private static final String IIO_PLUGIN_I18N = "iio-plugin.properties";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AWT);
    }

    /**
     * TODO: Is there a better way to disable an extension's native build on a particular platform?
     */
    @BuildStep(onlyIf = NativeBuild.class)
    @Produce(NativeImageResourceBuildItem.class)
    public void osSupportCheck(NativeConfig nativeConfig) {
        if (SystemUtils.IS_OS_WINDOWS && !nativeConfig.isContainerBuild()) {
            throw new UnsupportedOperationException(
                    "Windows AWT integration is not ready in native-image and would result in " +
                            "java.lang.UnsatisfiedLinkError: no awt in java.library.path.");
        }
    }

    /**
     * TODO: This warning pops in the log *AFTER* the whole ordeal of native-image build
     * is done to inform the user that the version of Java used to build the native-image
     * toolchain is sketchy. That is annoying.
     *
     * It seems to be a chicken-egg problem: I cannot both have NativeImageBuildItem instance
     * with getGraalVMInfo in it AND run the code before NativeImageBuild...
     * Attempts to do so end up in creating cycles in build step pipeline.
     *
     * Do I have to go and refactor NativeImageBuildItem to move GraalVMInfo up the chain,
     * to e.g. NativeImageResourceBuildItem...?
     */
    @BuildStep(onlyIf = NativeBuild.class)
    @Produce(ArtifactResultBuildItem.class)
    public void jdkVersionCheck(NativeImageBuildItem image) {
        final Matcher m = JDK_VERSION_FROM_NATIVE_IMAGE.matcher(image.getGraalVMInfo().getFullVersion());
        if (m.matches()) {
            final int feature = Integer.parseInt(m.group(2));
            final int update = Integer.parseInt(m.group(4));
            if (feature == 11 && update < 13) {
                LOGGER.warn(m.group(1) + " used with the native-image distribution is too old. " +
                        "Some MLib related operations, such as filter in " +
                        "awt.image.ConvolveOp will not work. See https://bugs.openjdk.java.net/browse/JDK-8254024");
            }
        }
    }

    /**
     * TODO
     *
     * The problem is that even if we bring I18N into the fold , e.g.
     * runtimeInit.accept(new RuntimeInitializedClassBuildItem("com.sun.imageio.plugins.common.I18N"));
     * (or ReflectiveClassBuildItem on the calling class, e.g. BMPReader)
     *
     * so as this works:
     * https://github.com/openjdk/jdk11u-dev/blob/jdk-11.0.13%2B7/src/java.desktop/share/classes/com/sun/imageio/plugins/common/I18NImpl.java#L54
     *
     * there is no such property file in the native image:
     * https://github.com/openjdk/jdk11u-dev/blob/jdk-11.0.13%2B7/src/java.desktop/share/classes/com/sun/imageio/plugins/common/iio-plugin.properties
     *
     * hence, it ends up with a NPE anyway.
     *
     * Trying to bring it in as a NativeImageResourceBuildItem does not help, likely due to it
     * being in an unexported module? i.e. all this is fruitless:
     *
     * .produce(new NativeImageResourceBuildItem("iio-plugin.properties"));
     * .produce(new NativeImageResourceBuildItem("classes/com/sun/imageio/plugins/common/iio-plugin.properties"));
     * .produce(new NativeImageResourceBuildItem("com/sun/imageio/plugins/common/iio-plugin.properties"));
     * .produce(new NativeImageResourceBuildItem("/iio-plugin.properties"));
     * .produce(new NativeImageResourceBuildItem("/classes/com/sun/imageio/plugins/common/iio-plugin.properties"));
     * .produce(new NativeImageResourceBuildItem("/com/sun/imageio/plugins/common/iio-plugin.properties"));
     *
     * Current workaround:
     *
     * The undermentioned code yanks the property file from the host JDK and bakes it into the image
     * and the accompanying substitution Target_com_sun_imageio_plugins_common_I18NImpl loads it.
     *
     * "It Works", although it is potentially problematic as your GRAALVM_HOME might not be your JAVA_HOME,
     * so your iio-plugin.properties might come from e.g. JDK 11.0.x you run the maven build with,
     * although the classes in com.sun.imageio.plugins* will come from your GRAALVM_HOME, having
     * native-image potentially built with a very different JDK version.
     *
     * Even if your local JAVA_HOME and GRAALVM_HOME is the same, there is still a discrepancy when
     * executed in a container builder image: Again, com.sun.imageio.plugins* classes come from the
     * Mandrel/Graal distro in the container, while your iio-plugin.properties comes from the local
     * JDK.
     *
     * Example:
     *
     * Test run of:
     *
     * ./mvnw -o clean verify -f integration-tests/pom.xml -pl awt -Pnative -Dquarkus.native.container-build=true \
     * -Dquarkus.native.builder-image=registry.access.redhat.com/quarkus/mandrel-21-rhel8:latest
     *
     * intentionally triggers an error that should have a message associated with it from the iio-plugin.properties file.
     * It works as expected, although the file was taken from a local host JDK I have purposely modified to append "KARM"
     * to the message:
     *
     * Caused by: javax.imageio.IIOException: New BMP version not implemented yet KARM.
     * at com.sun.imageio.plugins.bmp.BMPImageReader.readHeader(BMPImageReader.java:584)
     * at com.sun.imageio.plugins.bmp.BMPImageReader.read(BMPImageReader.java:830)
     *
     * TODO: I would like to take the property file from the Graal/Mandrel distro, always, regardless of the build
     * being done locally or in a container.
     */
    @BuildStep
    GeneratedResourceBuildItem yankI18NPropertiesFromHostJDK(BuildProducer<NativeImageResourceBuildItem> nibProducer,
            NativeConfig nativeConfig)
            throws IOException {

        /*
         * Nicer, does the same, but does not work with JDK17
         * try (final InputStream i = Class.forName("com.sun.imageio.plugins.common.I18N").getResourceAsStream(IIO_PLUGIN_I18N);
         * final ByteArrayOutputStream o = new ByteArrayOutputStream()) {
         * o.writeBytes(Objects.requireNonNull(i, IIO_PLUGIN_I18N + " not found").readAllBytes());
         * nibProducer.produce(new NativeImageResourceBuildItem(IIO_PLUGIN_I18N));
         * return new GeneratedResourceBuildItem(IIO_PLUGIN_I18N, o.toByteArray());
         * }
         */

        final Path pathToModule = Path.of(nativeConfig.graalvmHome.orElse(nativeConfig.javaHome.getAbsolutePath()),
                "jmods", "java.desktop.jmod");
        try (final FileSystem fileSystem = FileSystems.newFileSystem(pathToModule, null);
                final ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.writeBytes(Files.readAllBytes(
                    fileSystem.getPath("classes/com/sun/imageio/plugins/common/" + IIO_PLUGIN_I18N)));
            nibProducer.produce(new NativeImageResourceBuildItem(IIO_PLUGIN_I18N));
            return new GeneratedResourceBuildItem(IIO_PLUGIN_I18N, out.toByteArray());
        }

    }

    @BuildStep
    ReflectiveClassBuildItem setupReflectionClasses() {
        return new ReflectiveClassBuildItem(false, false,

                "sun.awt.X11.XToolkit",
                "sun.awt.X11FontManager",
                "sun.awt.X11GraphicsEnvironment");
    }

    @BuildStep
    ReflectiveClassBuildItem setupReflectionClassesWithMethods() {
        return new ReflectiveClassBuildItem(true, false,
                "sun.java2d.loops.SetDrawLineANY",
                "sun.java2d.loops.SetDrawPathANY",
                "sun.java2d.loops.SetDrawPolygonsANY",
                "sun.java2d.loops.SetDrawRectANY",
                "sun.java2d.loops.SetFillPathANY",
                "sun.java2d.loops.SetFillRectANY",
                "sun.java2d.loops.SetFillSpansANY",
                "sun.java2d.loops.OpaqueCopyAnyToArgb",
                "sun.java2d.loops.OpaqueCopyArgbToAny",
                "javax.imageio.plugins.tiff.BaselineTIFFTagSet",
                "javax.imageio.plugins.tiff.ExifGPSTagSet",
                "javax.imageio.plugins.tiff.ExifInteroperabilityTagSet",
                "javax.imageio.plugins.tiff.ExifParentTIFFTagSet",
                "javax.imageio.plugins.tiff.ExifTIFFTagSet",
                "javax.imageio.plugins.tiff.FaxTIFFTagSet",
                "javax.imageio.plugins.tiff.GeoTIFFTagSet",
                "javax.imageio.plugins.tiff.TIFFTagSet");
    }

    @BuildStep
    JniRuntimeAccessBuildItem setupJava2DClasses() {
        return new JniRuntimeAccessBuildItem(true, true, true,
                "com.sun.imageio.plugins.jpeg.JPEGImageReader",
                "com.sun.imageio.plugins.jpeg.JPEGImageWriter",
                "java.awt.AlphaComposite",
                "java.awt.Color",
                "java.awt.color.CMMException",
                "java.awt.color.ColorSpace",
                "java.awt.color.ICC_Profile",
                "java.awt.color.ICC_ProfileGray",
                "java.awt.color.ICC_ProfileRGB",
                "java.awt.geom.AffineTransform",
                "java.awt.geom.GeneralPath",
                "java.awt.geom.Path2D",
                "java.awt.geom.Path2D$Float",
                "java.awt.geom.Point2D$Float",
                "java.awt.geom.Rectangle2D$Float",
                "java.awt.image.AffineTransformOp",
                "java.awt.image.BandedSampleModel",
                "java.awt.image.BufferedImage",
                "java.awt.image.ColorModel",
                "java.awt.image.ComponentColorModel",
                "java.awt.image.ComponentSampleModel",
                "java.awt.image.ConvolveOp",
                "java.awt.image.DirectColorModel",
                "java.awt.image.IndexColorModel",
                "java.awt.image.Kernel",
                "java.awt.image.MultiPixelPackedSampleModel",
                "java.awt.image.PackedColorModel",
                "java.awt.image.PixelInterleavedSampleModel",
                "java.awt.image.Raster",
                "java.awt.image.SampleModel",
                "java.awt.image.SinglePixelPackedSampleModel",
                "java.awt.Transparency",
                "javax.imageio.IIOException",
                "javax.imageio.plugins.jpeg.JPEGHuffmanTable",
                "javax.imageio.plugins.jpeg.JPEGQTable",
                "sun.awt.image.BufImgSurfaceData",
                "sun.awt.image.BufImgSurfaceData$ICMColorData",
                "sun.awt.image.ByteBandedRaster",
                "sun.awt.image.ByteComponentRaster",
                "sun.awt.image.ByteInterleavedRaster",
                "sun.awt.image.BytePackedRaster",
                "sun.awt.image.DataBufferNative",
                "sun.awt.image.GifImageDecoder",
                "sun.awt.image.ImageRepresentation",
                "sun.awt.image.ImagingLib",
                "sun.awt.image.IntegerComponentRaster",
                "sun.awt.image.IntegerInterleavedRaster",
                "sun.awt.image.ShortBandedRaster",
                "sun.awt.image.ShortComponentRaster",
                "sun.awt.image.ShortInterleavedRaster",
                "sun.awt.image.SunWritableRaster",
                "sun.awt.image.WritableRasterNative",
                "sun.awt.SunHints",
                "sun.font.CharToGlyphMapper",
                "sun.font.Font2D",
                "sun.font.FontConfigManager",
                "sun.font.FontManagerNativeLibrary",
                "sun.font.FontStrike",
                "sun.font.FreetypeFontScaler",
                "sun.font.GlyphLayout",
                "sun.font.GlyphLayout$EngineRecord",
                "sun.font.GlyphLayout$GVData",
                "sun.font.GlyphLayout$LayoutEngine",
                "sun.font.GlyphLayout$LayoutEngineFactory",
                "sun.font.GlyphLayout$LayoutEngineKey",
                "sun.font.GlyphLayout$SDCache",
                "sun.font.GlyphLayout$SDCache$SDKey",
                "sun.font.GlyphList",
                "sun.font.PhysicalStrike",
                "sun.font.StrikeMetrics",
                "sun.font.TrueTypeFont",
                "sun.font.Type1Font",
                "sun.java2d.cmm.lcms.LCMSImageLayout",
                "sun.java2d.cmm.lcms.LCMSProfile",
                "sun.java2d.cmm.lcms.LCMSTransform",
                "sun.java2d.DefaultDisposerRecord",
                "sun.java2d.Disposer",
                "sun.java2d.InvalidPipeException",
                "sun.java2d.loops.Blit",
                "sun.java2d.loops.BlitBg",
                "sun.java2d.loops.CompositeType",
                "sun.java2d.loops.DrawGlyphList",
                "sun.java2d.loops.DrawGlyphListAA",
                "sun.java2d.loops.DrawGlyphListLCD",
                "sun.java2d.loops.DrawLine",
                "sun.java2d.loops.DrawParallelogram",
                "sun.java2d.loops.DrawPath",
                "sun.java2d.loops.DrawPolygons",
                "sun.java2d.loops.DrawRect",
                "sun.java2d.loops.FillParallelogram",
                "sun.java2d.loops.FillPath",
                "sun.java2d.loops.FillRect",
                "sun.java2d.loops.FillSpans",
                "sun.java2d.loops.GraphicsPrimitive",
                "sun.java2d.loops.GraphicsPrimitive[]",
                "sun.java2d.loops.GraphicsPrimitiveMgr",
                "sun.java2d.loops.MaskBlit",
                "sun.java2d.loops.MaskFill",
                "sun.java2d.loops.ScaledBlit",
                "sun.java2d.loops.SurfaceType",
                "sun.java2d.loops.TransformHelper",
                "sun.java2d.loops.XORComposite",
                "sun.java2d.NullSurfaceData",
                "sun.java2d.pipe.BufferedMaskBlit",
                "sun.java2d.pipe.GlyphListPipe",
                "sun.java2d.pipe.Region",
                "sun.java2d.pipe.RegionIterator",
                "sun.java2d.pipe.ShapeSpanIterator",
                "sun.java2d.pipe.SpanClipRenderer",
                "sun.java2d.pipe.ValidatePipe",
                "sun.java2d.SunGraphics2D",
                "sun.java2d.SurfaceData");
    }
}
