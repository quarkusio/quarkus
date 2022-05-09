package io.quarkus.awt.deployment;

import static io.quarkus.deployment.builditem.nativeimage.UnsupportedOSBuildItem.Os.WINDOWS;

import io.quarkus.deployment.Feature;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourcePatternsBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeMinimalJavaVersionBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.UnsupportedOSBuildItem;
import io.quarkus.deployment.pkg.steps.NativeBuild;
import io.quarkus.deployment.pkg.steps.NativeOrNativeSourcesBuild;

class AwtProcessor {

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(Feature.AWT);
    }

    @BuildStep(onlyIf = NativeBuild.class)
    UnsupportedOSBuildItem osSupportCheck() {
        return new UnsupportedOSBuildItem(WINDOWS,
                "Windows AWT integration is not ready in native-image and would result in " +
                        "java.lang.UnsatisfiedLinkError: no awt in java.library.path.");
    }

    @BuildStep(onlyIf = NativeBuild.class)
    NativeMinimalJavaVersionBuildItem nativeMinimalJavaVersionBuildItem() {
        return new NativeMinimalJavaVersionBuildItem(11, 13,
                "AWT: Some MLib related operations, such as filter in awt.image.ConvolveOp will not work. " +
                        "See https://bugs.openjdk.java.net/browse/JDK-8254024");
    }

    @BuildStep(onlyIf = NativeOrNativeSourcesBuild.class)
    void i18NProperties(
            BuildProducer<NativeImageResourcePatternsBuildItem> resourcePatternsBuildItemBuildProducer) {
        resourcePatternsBuildItemBuildProducer
                .produce(NativeImageResourcePatternsBuildItem.builder().includePattern(".*/iio-plugin.*properties$").build());
    }

    @BuildStep
    ReflectiveClassBuildItem setupReflectionClasses() {
        return new ReflectiveClassBuildItem(false, false,
                "sun.awt.X11.XToolkit",
                "sun.awt.X11FontManager",
                "sun.awt.X11GraphicsEnvironment",
                "com.sun.imageio.plugins.common.I18N");
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
