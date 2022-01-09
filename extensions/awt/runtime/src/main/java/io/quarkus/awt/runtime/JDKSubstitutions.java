package io.quarkus.awt.runtime;

import java.awt.FontFormatException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.PropertyResourceBundle;

import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * TODO: This is just a workaround,
 * see AwtProcessor#yankI18NPropertiesFromHostJDK
 */
@TargetClass(className = "com.sun.imageio.plugins.common.I18NImpl")
final class Target_com_sun_imageio_plugins_common_I18NImpl {

    @Substitute
    private static String getString(String className, String resource_name, String key) {
        PropertyResourceBundle bundle = null;
        try {
            // className ignored, there is only one such file in the imageio anyway
            InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource_name);
            bundle = new PropertyResourceBundle(stream);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return (String) bundle.handleGetObject(key);
    }
}

/**
 * Getting .pfb/.pfa files to work would require additional runtime re-init adjustments.
 * We are not doing that unless there is an explicit demand.
 */
@TargetClass(className = "sun.font.Type1Font")
final class Target_sun_font_Type1Font {
    @Substitute
    private void verifyPFA(ByteBuffer bb) throws FontFormatException {
        throw new FontFormatException(
                ".pfa font files are not supported. Use TrueType fonts, i.e. .ttf files.");
    }

    @Substitute
    private void verifyPFB(ByteBuffer bb) throws FontFormatException {
        throw new FontFormatException(
                ".pfb font files are not supported. Use TrueType fonts, i.e. .ttf files.");
    }
}

public class JDKSubstitutions {
}
