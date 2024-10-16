package io.quarkus.awt.deployment;

import static io.quarkus.deployment.builditem.nativeimage.UnsupportedOSBuildItem.Os.WINDOWS;
import static io.quarkus.deployment.pkg.steps.GraalVM.Version.CURRENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.awt.runtime.graal.DarwinAwtFeature;
import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.NativeImageFeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessFieldBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessMethodBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedPackageBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsupportedOSBuildItem;
import io.quarkus.deployment.pkg.builditem.NativeImageRunnerBuildItem;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabled;
import io.quarkus.deployment.pkg.builditem.ProcessInheritIODisabledBuildItem;
import io.quarkus.deployment.pkg.steps.GraalVM;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.pkg.steps.NoopNativeImageBuildRunner;

class AwtProcessor {

    private static final Logger log = Logger.getLogger(AwtProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AWT);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeImageFeatures(BuildProducer<NativeImageFeatureBuildItem> nativeImageFeatures) {
        nativeImageFeatures.produce(new NativeImageFeatureBuildItem(DarwinAwtFeature.class));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    UnsupportedOSBuildItem osSupportCheck() {
        return new UnsupportedOSBuildItem(WINDOWS,
                "Windows AWT integration is not ready in native-image and would result in " +
                        "java.lang.UnsatisfiedLinkError: no awt in java.library.path.");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void resources(
            BuildProducer<NativeImageResourcePatternsBuildItem> resourcePatternsBuildItemBuildProducer) {
        resourcePatternsBuildItemBuildProducer
                .produce(NativeImageResourcePatternsBuildItem.builder()
                        .includePattern(".*/iio-plugin.*properties$") // Texts for e.g. exceptions strings
                        .includePattern(".*/.*pf$") // Default colour profiles
                        .build());
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    ReflectiveClassBuildItem setupReflectionClasses() {
        return ReflectiveClassBuildItem.builder(
                "com.sun.imageio.plugins.common.I18N",
                "sun.awt.X11.XToolkit",
                "sun.awt.X11FontManager",
                "sun.awt.X11GraphicsEnvironment").build();
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    ReflectiveClassBuildItem setupReflectionClassesWithMethods() {
        //@formatter:off
        return ReflectiveClassBuildItem.builder(
                "javax.imageio.plugins.tiff.BaselineTIFFTagSet",
                "javax.imageio.plugins.tiff.ExifGPSTagSet",
                "javax.imageio.plugins.tiff.ExifInteroperabilityTagSet",
                "javax.imageio.plugins.tiff.ExifParentTIFFTagSet",
                "javax.imageio.plugins.tiff.ExifTIFFTagSet",
                "javax.imageio.plugins.tiff.FaxTIFFTagSet",
                "javax.imageio.plugins.tiff.GeoTIFFTagSet",
                "javax.imageio.plugins.tiff.TIFFTagSet",
                "sun.java2d.loops.OpaqueCopyAnyToArgb",
                "sun.java2d.loops.OpaqueCopyArgbToAny",
                "sun.java2d.loops.SetDrawLineANY",
                "sun.java2d.loops.SetDrawPathANY",
                "sun.java2d.loops.SetDrawPolygonsANY",
                "sun.java2d.loops.SetDrawRectANY",
                "sun.java2d.loops.SetFillPathANY",
                "sun.java2d.loops.SetFillRectANY",
                "sun.java2d.loops.SetFillSpansANY"
        ).methods().build();
        //@formatter:on
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void setupAWTInit(BuildProducer<JniRuntimeAccessBuildItem> jc,
            BuildProducer<JniRuntimeAccessMethodBuildItem> jm,
            BuildProducer<JniRuntimeAccessFieldBuildItem> jf,
            NativeImageRunnerBuildItem nativeImageRunnerBuildItem,
            Optional<ProcessInheritIODisabled> processInheritIODisabled,
            Optional<ProcessInheritIODisabledBuildItem> processInheritIODisabledBuildItem) {
        nativeImageRunnerBuildItem.getBuildRunner()
                .setup(processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
        // Dynamically loading shared objects instead
        // of baking in static libs: https://github.com/oracle/graal/issues/4921
        jm.produce(new JniRuntimeAccessMethodBuildItem("java.lang.System", "load", "java.lang.String"));
        jm.produce(
                new JniRuntimeAccessMethodBuildItem("java.lang.System", "setProperty", "java.lang.String",
                        "java.lang.String"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.SunToolkit", "awtLock"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.SunToolkit", "awtLockNotify"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.SunToolkit", "awtLockNotifyAll"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.SunToolkit", "awtLockWait", "long"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.SunToolkit", "awtUnlock"));
        jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.SunToolkit", "AWT_LOCK"));
        jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.SunToolkit", "AWT_LOCK_COND"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.X11.XErrorHandlerUtil", "init", "long"));
        jc.produce(new JniRuntimeAccessBuildItem(false, false, true, "sun.awt.X11.XToolkit"));
        jm.produce(new JniRuntimeAccessMethodBuildItem("java.lang.Thread", "yield"));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    JniRuntimeAccessBuildItem setupJava2DClasses(NativeImageRunnerBuildItem nativeImageRunnerBuildItem,
            Optional<ProcessInheritIODisabled> processInheritIODisabled,
            Optional<ProcessInheritIODisabledBuildItem> processInheritIODisabledBuildItem) {
        nativeImageRunnerBuildItem.getBuildRunner()
                .setup(processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
        final GraalVM.Version v;
        if (nativeImageRunnerBuildItem.getBuildRunner() instanceof NoopNativeImageBuildRunner) {
            v = CURRENT;
        } else {
            v = nativeImageRunnerBuildItem.getBuildRunner().getGraalVMVersion();
        }
        final List<String> classes = new ArrayList<>();
        classes.add("com.sun.imageio.plugins.jpeg.JPEGImageReader");
        classes.add("com.sun.imageio.plugins.jpeg.JPEGImageWriter");
        classes.add("java.awt.AlphaComposite");
        classes.add("java.awt.Color");
        classes.add("java.awt.color.CMMException");
        classes.add("java.awt.color.ColorSpace");
        classes.add("java.awt.color.ICC_ColorSpace");
        classes.add("java.awt.color.ICC_Profile");
        classes.add("java.awt.color.ICC_ProfileGray");
        classes.add("java.awt.color.ICC_ProfileRGB");
        classes.add("java.awt.Composite");
        classes.add("java.awt.geom.AffineTransform");
        classes.add("java.awt.geom.GeneralPath");
        classes.add("java.awt.geom.Path2D");
        classes.add("java.awt.geom.Path2D$Float");
        classes.add("java.awt.geom.Point2D$Float");
        classes.add("java.awt.geom.Rectangle2D$Float");
        classes.add("java.awt.image.AffineTransformOp");
        classes.add("java.awt.image.BandedSampleModel");
        classes.add("java.awt.image.BufferedImage");
        classes.add("java.awt.image.ColorModel");
        classes.add("java.awt.image.ComponentColorModel");
        classes.add("java.awt.image.ComponentSampleModel");
        classes.add("java.awt.image.ConvolveOp");
        classes.add("java.awt.image.DirectColorModel");
        classes.add("java.awt.image.IndexColorModel");
        classes.add("java.awt.image.Kernel");
        classes.add("java.awt.image.MultiPixelPackedSampleModel");
        classes.add("java.awt.image.PackedColorModel");
        classes.add("java.awt.image.PixelInterleavedSampleModel");
        classes.add("java.awt.image.Raster");
        classes.add("java.awt.image.SampleModel");
        classes.add("java.awt.image.SinglePixelPackedSampleModel");
        classes.add("java.awt.Rectangle");
        classes.add("java.awt.Transparency");
        classes.add("javax.imageio.IIOException");
        classes.add("javax.imageio.plugins.jpeg.JPEGHuffmanTable");
        classes.add("javax.imageio.plugins.jpeg.JPEGQTable");
        classes.add("sun.awt.image.BufImgSurfaceData");
        classes.add("sun.awt.image.BufImgSurfaceData$ICMColorData");
        classes.add("sun.awt.image.ByteBandedRaster");
        classes.add("sun.awt.image.ByteComponentRaster");
        classes.add("sun.awt.image.ByteInterleavedRaster");
        classes.add("sun.awt.image.BytePackedRaster");
        classes.add("sun.awt.image.DataBufferNative");
        classes.add("sun.awt.image.GifImageDecoder");
        classes.add("sun.awt.image.ImageRepresentation");
        classes.add("sun.awt.image.ImagingLib");
        classes.add("sun.awt.image.IntegerComponentRaster");
        classes.add("sun.awt.image.IntegerInterleavedRaster");
        classes.add("sun.awt.image.ShortBandedRaster");
        classes.add("sun.awt.image.ShortComponentRaster");
        classes.add("sun.awt.image.ShortInterleavedRaster");
        classes.add("sun.awt.image.SunWritableRaster");
        classes.add("sun.awt.image.WritableRasterNative");
        classes.add("sun.awt.SunHints");
        classes.add("sun.font.CharToGlyphMapper");
        classes.add("sun.font.Font2D");
        classes.add("sun.font.FontConfigManager");
        classes.add("sun.font.FontConfigManager$FcCompFont");
        classes.add("sun.font.FontConfigManager$FontConfigFont");
        classes.add("sun.font.FontConfigManager$FontConfigInfo");
        classes.add("sun.font.FontManagerNativeLibrary");
        classes.add("sun.font.FontStrike");
        classes.add("sun.font.FreetypeFontScaler");
        classes.add("sun.font.GlyphLayout");
        classes.add("sun.font.GlyphLayout$EngineRecord");
        classes.add("sun.font.GlyphLayout$GVData");
        classes.add("sun.font.GlyphLayout$LayoutEngine");
        classes.add("sun.font.GlyphLayout$LayoutEngineFactory");
        classes.add("sun.font.GlyphLayout$LayoutEngineKey");
        classes.add("sun.font.GlyphLayout$SDCache");
        classes.add("sun.font.GlyphLayout$SDCache$SDKey");
        classes.add("sun.font.GlyphList");
        classes.add("sun.font.PhysicalStrike");
        classes.add("sun.font.StrikeMetrics");
        classes.add("sun.font.TrueTypeFont");
        classes.add("sun.font.Type1Font");
        classes.add("sun.java2d.cmm.lcms.LCMS");
        classes.add("sun.java2d.cmm.lcms.LCMSImageLayout");
        classes.add("sun.java2d.cmm.lcms.LCMSProfile");
        classes.add("sun.java2d.cmm.lcms.LCMSTransform");
        classes.add("sun.java2d.DefaultDisposerRecord");
        classes.add("sun.java2d.Disposer");
        classes.add("sun.java2d.InvalidPipeException");
        classes.add("sun.java2d.loops.Blit");
        classes.add("sun.java2d.loops.BlitBg");
        classes.add("sun.java2d.loops.CompositeType");
        classes.add("sun.java2d.loops.DrawGlyphList");
        classes.add("sun.java2d.loops.DrawGlyphListAA");
        classes.add("sun.java2d.loops.DrawGlyphListLCD");
        classes.add("sun.java2d.loops.DrawLine");
        classes.add("sun.java2d.loops.DrawParallelogram");
        classes.add("sun.java2d.loops.DrawPath");
        classes.add("sun.java2d.loops.DrawPolygons");
        classes.add("sun.java2d.loops.DrawRect");
        classes.add("sun.java2d.loops.FillParallelogram");
        classes.add("sun.java2d.loops.FillPath");
        classes.add("sun.java2d.loops.FillRect");
        classes.add("sun.java2d.loops.FillSpans");
        classes.add("sun.java2d.loops.GraphicsPrimitive");
        classes.add("sun.java2d.loops.GraphicsPrimitiveMgr");
        classes.add("sun.java2d.loops.MaskBlit");
        classes.add("sun.java2d.loops.MaskFill");
        classes.add("sun.java2d.loops.ScaledBlit");
        classes.add("sun.java2d.loops.SurfaceType");
        classes.add("sun.java2d.loops.TransformHelper");
        classes.add("sun.java2d.loops.XORComposite");
        classes.add("sun.java2d.NullSurfaceData");
        classes.add("sun.java2d.pipe.BufferedMaskBlit");
        classes.add("sun.java2d.pipe.GlyphListPipe");
        classes.add("sun.java2d.pipe.Region");
        classes.add("sun.java2d.pipe.RegionIterator");
        classes.add("sun.java2d.pipe.ShapeSpanIterator");
        classes.add("sun.java2d.pipe.SpanClipRenderer");
        classes.add("sun.java2d.pipe.SpanIterator");
        classes.add("sun.java2d.pipe.ValidatePipe");
        classes.add("sun.java2d.SunGraphics2D");
        classes.add("sun.java2d.SurfaceData");

        // A new way of dynamically loading shared objects instead
        // of baking in static libs: https://github.com/oracle/graal/issues/4921
        classes.add("sun.awt.X11FontManager");
        if (v.javaVersion.feature() != 19) {
            classes.add("java.awt.GraphicsEnvironment");
            classes.add("sun.awt.X11GraphicsConfig");
            classes.add("sun.awt.X11GraphicsDevice");
            classes.add("sun.java2d.SunGraphicsEnvironment");
            classes.add("sun.java2d.xr.XRSurfaceData");
        }

        // Added for JDK 19+ due to: https://github.com/openjdk/jdk20/commit/9bc023220 calling FontUtilities
        if (v.jdkVersionGreaterOrEqualTo("19")) {
            classes.add("sun.font.FontUtilities");
        }

        return new JniRuntimeAccessBuildItem(true, true, true, classes.toArray(new String[0]));
    }

    /*
     * Moved over here due to: https://github.com/quarkusio/quarkus/pull/32069
     * A better detection and DarwinAwtFeature handling might be in order.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitilizedPackages) {
        /*
         * Note that this initialization is not enough if user wants to deserialize actual images
         * (e.g. from XML). AWT Extension must be loaded for decoding JDK supported image formats.
         */
        //@formatter:off
        Stream.of(
                "com.sun.imageio",
                "java.awt",
                "javax.imageio",
                "sun.awt",
                "sun.font",
                "sun.java2d")
                .map(RuntimeInitializedPackageBuildItem::new)
                .forEach(runtimeInitilizedPackages::produce);
        //@formatter:on
    }
}
