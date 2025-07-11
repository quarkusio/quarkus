package io.quarkus.awt.runtime;

import java.awt.FontFormatException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

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

@TargetClass(className = "sun.awt.FontConfiguration")
final class Target_sun_awt_FontConfiguration {
    @Alias
    protected static String osVersion;
    @Alias
    protected static String osName;

    /**
     * AWT source code does not take into account a situation where "java.home" does not
     * exist. It looks for default fonts in conf/fonts and lib dirs. It is O.K. if there are
     * none as then system fonts are used instead. If the directory structure as such does not exist,
     * the code path fails though.
     *
     * We create a dummy "java.home" in "java.io.tmpdir" and we set it at a reasonable place via
     * substitution.
     */
    @Substitute
    protected void setOsNameAndVersion() {
        final Path javaHome = Path.of(System.getProperty("java.io.tmpdir"), "quarkus-awt-tmp-fonts");
        try {
            Files.createDirectories(Path.of(javaHome.toString(), "conf", "fonts"));
            Files.createDirectories(Path.of(javaHome.toString(), "lib"));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to set tmp java.home for FontConfig Quarkus AWT usage in " + javaHome, e);
        }
        System.setProperty("java.home", javaHome.toString());
        osName = System.getProperty("os.name");
        osVersion = System.getProperty("os.version");
    }
}

/**
 * We get rid of ResourceBundle.getBundle(...) for
 * sun.awt.resources.awt
 * sun/awt/resources/awt.properties
 * sun.awt.resources.awt_en
 * sun/awt/resources/awt_en.properties
 * sun.awt.resources.awt_en_US
 * sun/awt/resources/awt_en_US.properties
 * we don't include those bundles anyway, so the search is in vain,
 * and we don't need those as they contain strings for GUI elements,
 * useless in our exclusively headless setup.
 * See https://github.com/quarkusio/quarkus/issues/44622
 */
@TargetClass(className = "java.awt.Toolkit$4")
final class Target_java_awt_Toolkit$4 {
    @Substitute
    public Void run() {
        return null;
    }
}

/**
 * TODO: This is perhaps too harsh, is it needed?
 * See See https://github.com/quarkusio/quarkus/issues/44622
 */
@TargetClass(className = "javax.imageio.spi.IIORegistry")
final class Target_javax_imageio_spi_IIORegistry {
    @Substitute
    private void registerInstalledProviders() {
        // Do nothing. We don't support ImageIO plugins via META-INF/services anyway.
    }

    @Substitute
    private void registerApplicationClasspathSpis() {
        // Do nothing. We don't support ImageIO plugins found on classpath.
    }
}

public class JDKSubstitutions {
}
