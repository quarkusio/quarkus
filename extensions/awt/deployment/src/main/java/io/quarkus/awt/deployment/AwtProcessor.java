package io.quarkus.awt.deployment;

import static io.quarkus.runtime.graal.GraalVM.Version.CURRENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.jboss.logging.Logger;

import io.quarkus.awt.runtime.graal.AwtFeature;
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
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;
import io.quarkus.deployment.pkg.steps.NoopNativeImageBuildRunner;
import io.quarkus.runtime.graal.GraalVM;
import io.smallrye.common.cpu.CPU;
import io.smallrye.common.os.OS;

class AwtProcessor {

    private static final Logger log = Logger.getLogger(AwtProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AWT);
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void nativeImageFeatures(BuildProducer<NativeImageFeatureBuildItem> nativeImageFeatures) {
        nativeImageFeatures.produce(new NativeImageFeatureBuildItem(DarwinAwtFeature.class));
        nativeImageFeatures.produce(new NativeImageFeatureBuildItem(AwtFeature.class));
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void supportCheck(BuildProducer<UnsupportedOSBuildItem> unsupported,
            NativeImageRunnerBuildItem nativeImageRunnerBuildItem) {
        unsupported.produce(new UnsupportedOSBuildItem(OS.MAC,
                "MacOS AWT integration is not ready in Quarkus native-image and would result in " +
                        "java.lang.UnsatisfiedLinkError: Can't load library: awt | java.library.path = [.]."));
        final GraalVM.Version v;
        if (nativeImageRunnerBuildItem.getBuildRunner() instanceof NoopNativeImageBuildRunner) {
            v = CURRENT;
            log.warnf("native-image is not installed. " +
                    "Using the default %s version as a reference to build native-sources step.", v.getVersionAsString());
        } else {
            v = nativeImageRunnerBuildItem.getBuildRunner().getGraalVMVersion();
        }

        if (v.compareTo(io.quarkus.deployment.pkg.steps.GraalVM.Version.VERSION_24_2_0) >= 0
                && v.compareTo(GraalVM.Version.VERSION_25_0_0) < 0) {
            unsupported.produce(new UnsupportedOSBuildItem(CPU.aarch64,
                    "AWT needs JDK's JEP 454 FFI/FFM support and that is not available for AArch64 with " +
                            "GraalVM's native-image prior to JDK 25, see: " +
                            "https://www.graalvm.org/latest/reference-manual/native-image/native-code-interoperability/foreign-interface/#foreign-functions"));
        }

    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void resources(
            BuildProducer<NativeImageResourcePatternsBuildItem> resourcePatternsBuildItemBuildProducer) {
        resourcePatternsBuildItemBuildProducer
                .produce(NativeImageResourcePatternsBuildItem.builder()
                        .includeGlobs("**/iio-plugin*.properties", // Texts for e.g. exceptions strings
                                "**/*.pf") // Default colour profiles
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

        if (OS.WINDOWS.isCurrent()) {
            // Needed by the native method sun.awt.Win32FontManager#populateFontFileNameMap0
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.util.HashMap", "put", "java.lang.Object", "java.lang.Object"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.util.HashMap", "containsKey", "java.lang.Object"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.util.ArrayList", "<init>", "int"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.util.ArrayList", "add", "java.lang.Object"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.lang.String", "toLowerCase", "java.util.Locale"));

            // Needed by the native method java.awt.Toolkit#initIDs
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Toolkit", "getDefaultToolkit"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Toolkit", "getFontMetrics", "java.awt.Font"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Insets", "<init>", "int", "int", "int", "int"));

            // Needed by the native method java.awt.Insets#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Insets", "left"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Insets", "right"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Insets", "top"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Insets", "bottom"));

            // Needed by the native method sun.awt.windows.WToolkit#initIDs
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WToolkit", "windowsSettingChange"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WToolkit", "displayChanged"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.image.SunVolatileImage", "volSurfaceManager"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.image.VolatileSurfaceManager", "sdCurrent"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.java2d.SurfaceData", "pData"));
            jc.produce(new JniRuntimeAccessBuildItem(false, false, false, "java.awt.Component"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.desktop.UserSessionEvent$Reason", "UNSPECIFIED"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.desktop.UserSessionEvent$Reason", "CONSOLE"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.desktop.UserSessionEvent$Reason", "REMOTE"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.desktop.UserSessionEvent$Reason", "LOCK"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WDesktopPeer", "systemSleepCallback", "boolean"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WDesktopPeer", "userSessionCallback", "boolean",
                    "java.awt.desktop.UserSessionEvent$Reason"));

            // Needed by the native method java.awt.Component#initIDs
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.event.InputEvent", "getButtonDownMasks"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "peer"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "x"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "y"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "height"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "width"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "visible"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "background"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "foreground"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "enabled"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "parent"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "graphicsConfig"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "focusable"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "appContext"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Component", "cursor"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Component", "getFont_NoClientCode"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Component", "getToolkitImpl"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Component", "isEnabledImpl"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Component", "getLocationOnScreen_NoTreeLock"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WComponentPeer", "winGraphicsConfig"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WComponentPeer", "hwnd"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WComponentPeer", "replaceSurfaceData"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WComponentPeer", "replaceSurfaceDataLater"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WComponentPeer", "disposeLater"));

            // Needed by the native method java.awt.AWTEvent#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.AWTEvent", "bdata"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.AWTEvent", "id"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.AWTEvent", "consumed"));

            // Needed by the native method java.awt.event.InputEvent#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.event.InputEvent", "modifiers"));

            // Needed by the native method sun.awt.windows.WObjectPeer#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WObjectPeer", "pData"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WObjectPeer", "destroyed"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WObjectPeer", "target"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WObjectPeer", "createError"));
            jm.produce(
                    new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WObjectPeer", "getPeerForTarget", "java.lang.Object"));

            // Needed by the native method java.awt.Font#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Font", "pData"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Font", "name"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Font", "size"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Font", "style"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Font", "getFontPeer"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.Font", "getFont", "java.lang.String"));

            // Needed by the native method sun.java2d.windows.WindowsFlags#initNativeFlags
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.java2d.windows.WindowsFlags", "d3dEnabled"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.java2d.windows.WindowsFlags", "d3dSet"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.java2d.windows.WindowsFlags", "setHighDPIAware"));

            // Needed by the native method sun.awt.Win32GraphicsEnvironment#initDisplay
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.Win32GraphicsEnvironment", "dwmCompositionChanged",
                    "boolean"));

            // Needed by the native method java.awt.Dimension#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Dimension", "width"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.Dimension", "height"));

            // Needed by the native method java.awt.FontMetrics#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.FontMetrics", "font"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("java.awt.FontMetrics", "getHeight"));

            // Needed by the native method sun.awt.FontDescriptor#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.FontDescriptor", "nativeName"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.FontDescriptor", "useUnicode"));

            // Needed by the native method sun.awt.PlatformFont#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.PlatformFont", "fontConfig"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.PlatformFont", "componentFonts"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.PlatformFont", "makeConvertedMultiFontString",
                    "java.lang.String"));

            // Needed by the native method sun.awt.windows.WFontPeer#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontPeer", "textComponentFontName"));

            // Needed by the native method sun.awt.windows.WDefaultFontCharset#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WDefaultFontCharset", "fontName"));

            // Needed by the native method sun.awt.windows.WFontMetrics#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "widths"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "ascent"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "descent"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "leading"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "height"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "maxAscent"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "maxDescent"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "maxHeight"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.windows.WFontMetrics", "maxAdvance"));

            // Needed by the native method sun.awt.Win32GraphicsDevice#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.Win32GraphicsDevice", "dynamicColorModel"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.image.IndexColorModel", "rgb"));
            jf.produce(new JniRuntimeAccessFieldBuildItem("java.awt.image.IndexColorModel", "lookupcache"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.windows.WToolkit", "paletteChanged"));

            // Needed by the native method sun.awt.windows.WToolkit#init
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.SunToolkit", "isTouchKeyboardAutoShowEnabled"));

            // Needed by the native method sun.awt.Win32GraphicsConfig#initIDs
            jf.produce(new JniRuntimeAccessFieldBuildItem("sun.awt.Win32GraphicsConfig", "visual"));

            // Needed by the native method sun.awt.windows.WToolkit#eventLoop
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.AWTAutoShutdown", "notifyToolkitThreadBusy"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.awt.AWTAutoShutdown", "notifyToolkitThreadFree"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.java2d.d3d.D3DGraphicsDevice$1", "run"));
            jm.produce(new JniRuntimeAccessMethodBuildItem("sun.java2d.d3d.D3DRenderQueue$1", "run"));
        }
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    JniRuntimeAccessBuildItem setupJava2DClasses(NativeImageRunnerBuildItem nativeImageRunnerBuildItem,
            Optional<ProcessInheritIODisabled> processInheritIODisabled,
            Optional<ProcessInheritIODisabledBuildItem> processInheritIODisabledBuildItem) {
        nativeImageRunnerBuildItem.getBuildRunner()
                .setup(processInheritIODisabled.isPresent() || processInheritIODisabledBuildItem.isPresent());
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
        classes.add("java.awt.GraphicsEnvironment");
        classes.add("sun.awt.X11GraphicsConfig");
        classes.add("sun.awt.X11GraphicsDevice");
        classes.add("sun.java2d.SunGraphicsEnvironment");
        classes.add("sun.java2d.xr.XRSurfaceData");

        return new JniRuntimeAccessBuildItem(true, true, true, classes.toArray(new String[0]));
    }

    /*
     * Moved over here due to: https://github.com/quarkusio/quarkus/pull/32069
     * A better detection and DarwinAwtFeature handling might be in order.
     */
    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void runtimeInitializedClasses(BuildProducer<RuntimeInitializedPackageBuildItem> runtimeInitializedPackages) {
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
                .forEach(runtimeInitializedPackages::produce);
        //@formatter:on
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    public void registerNativeImageResources(BuildProducer<NativeImageResourcePatternsBuildItem> resource) {
        resource.produce(
                NativeImageResourcePatternsBuildItem.builder().includeGlobs("/windows-fontconfig.properties").build());
    }
}
