package io.quarkus.runtime.graal;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.util.Iterator;
import java.util.function.BooleanSupplier;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.ServiceRegistry;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.jboss.logging.Logger;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;
import com.oracle.svm.core.annotate.TargetElement;

public class AwtImageIO {
    // The wording would be IS_WINDOWS and IS_MAC specific.
    public static String AWT_EXTENSION_HINT = "Add AWT Quarkus extension to enable Java2D/ImageIO. " +
            "Additional system libraries such as `freetype' and `fontconfig' might be needed.";
    static final Logger LOGGER = Logger.getLogger(AwtImageIO.class);

    /**
     * Detects if AWT extension is present on the classpath of the application.
     */
    static final class IsAWTAbsent implements BooleanSupplier {
        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("io.quarkus.awt.runtime.JDKSubstitutions");
                return false;
            } catch (ClassNotFoundException e) {
                return true;
            }
        }
    }
}

@TargetClass(className = "java.awt.GraphicsEnvironment", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_java_awt_GraphicsEnvironment {
    @Substitute
    public static GraphicsEnvironment getLocalGraphicsEnvironment() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }

    @Substitute
    public static boolean isHeadless() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "java.awt.Toolkit", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_java_awt_Toolkit {
    @Substitute
    public static synchronized Toolkit getDefaultToolkit() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "java.awt.Color", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_java_awt_Color {
    @Substitute
    private static void testColorValueRange(int r, int g, int b, int a) {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "javax.imageio.spi.IIORegistry", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_javax_imageio_spi_IIORegistry {
    @Substitute
    public static IIORegistry getDefaultInstance() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "javax.imageio.spi.ServiceRegistry", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_javax_imageio_spi_ServiceRegistry {
    @Substitute
    public <T> Iterator<T> getServiceProviders(Class<T> category, ServiceRegistry.Filter filter, boolean useOrdering) {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "java.awt.geom.AffineTransform", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_java_awt_geom_AffineTransform {
    @Substitute
    @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    void AffineTransform() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "java.awt.geom.Path2D", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_java_awt_geom_Path2D {
    @Substitute
    public final void setWindingRule(int rule) {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "java.awt.image.Kernel", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_java_awt_image_Kernel {
    @Substitute
    @TargetElement(name = TargetElement.CONSTRUCTOR_NAME)
    void Kernel(int width, int height, float[] data) {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "sun.font.FontManagerNativeLibrary", onlyWith = AwtImageIO.IsAWTAbsent.class)
@Substitute
final class Target_sun_font_FontManagerNativeLibrary {
    public static void load() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "sun.awt.FontConfiguration", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_sun_awt_FontConfiguration {
    @Substitute
    public synchronized boolean init() {
        throw new UnsupportedOperationException(AwtImageIO.AWT_EXTENSION_HINT);
    }
}

@TargetClass(className = "javax.imageio.ImageIO", onlyWith = AwtImageIO.IsAWTAbsent.class)
final class Target_javax_imageio_ImageIO {
    @Substitute
    public static ImageOutputStream createImageOutputStream(Object output) {
        // Exception would not emerge.
        AwtImageIO.LOGGER.error(AwtImageIO.AWT_EXTENSION_HINT);
        return null;
    }

    @Substitute
    public static ImageInputStream createImageInputStream(Object input) {
        // Exception would not emerge.
        AwtImageIO.LOGGER.error(AwtImageIO.AWT_EXTENSION_HINT);
        return null;
    }
}
