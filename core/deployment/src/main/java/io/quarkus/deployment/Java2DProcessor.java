package io.quarkus.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.JniRuntimeAccessBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

public class Java2DProcessor {

    @BuildStep
    ReflectiveClassBuildItem setupReflectionClasses() {
        return new ReflectiveClassBuildItem(false, false,
                "sun.awt.X11GraphicsEnvironment",
                "sun.awt.X11.XToolkit");
    }

    @BuildStep
    JniRuntimeAccessBuildItem setupJava2DClasses() {
        return new JniRuntimeAccessBuildItem(true, true, true,
                "java.awt.AlphaComposite",
                "java.awt.Color",
                "java.awt.geom.AffineTransform",
                "java.awt.geom.Path2D",
                "java.awt.geom.Path2D$Float",
                "java.awt.image.BufferedImage",
                "java.awt.image.ColorModel",
                "java.awt.image.IndexColorModel",
                "java.awt.image.Raster",
                "java.awt.image.SampleModel",
                "java.awt.image.SinglePixelPackedSampleModel",
                "sun.awt.SunHints",
                "sun.awt.image.BufImgSurfaceData$ICMColorData",
                "sun.awt.image.ByteComponentRaster",
                "sun.awt.image.BytePackedRaster",
                "sun.awt.image.ImageRepresentation",
                "sun.awt.image.IntegerComponentRaster",
                "sun.java2d.Disposer",
                "sun.java2d.InvalidPipeException",
                "sun.java2d.NullSurfaceData",
                "sun.java2d.SunGraphics2D",
                "sun.java2d.SurfaceData",
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
                "sun.java2d.loops.GraphicsPrimitiveMgr",
                "sun.java2d.loops.GraphicsPrimitive[]",
                "sun.java2d.loops.MaskBlit",
                "sun.java2d.loops.MaskFill",
                "sun.java2d.loops.ScaledBlit",
                "sun.java2d.loops.SurfaceType",
                "sun.java2d.loops.TransformHelper",
                "sun.java2d.loops.XORComposite",
                "sun.java2d.pipe.Region",
                "sun.java2d.pipe.RegionIterator");
    }

}
