package io.quarkus.awt.runtime;

import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.awt.PrintJob;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.util.IsLinux;
import io.quarkus.runtime.util.IsWindows;
import io.quarkus.runtime.util.JavaVersionLessThan25;

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

/**
 * AWT source code does not take into account a situation where "java.home" does not
 * exist. It looks for default fonts in conf/fonts and lib dirs. It is O.K. if there are
 * none as then system fonts are used instead. If the directory structure as such does not exist,
 * the code path fails though.
 *
 * We create a dummy "java.home" in "java.io.tmpdir" and we set it at a reasonable place via
 * substitution.
 */
@TargetClass(className = "sun.awt.FontConfiguration", onlyWith = IsLinux.class)
final class Target_sun_awt_FontConfiguration_Linux {
    @Alias
    protected static String osVersion;
    @Alias
    protected static String osName;

    @Substitute
    protected void setOsNameAndVersion() {
        final Path javaHome = Path.of(System.getProperty("java.io.tmpdir"), "quarkus-awt-tmp-fonts");
        try {
            System.setProperty("java.home", javaHome.toString());
            osName = System.getProperty("os.name", "unknown");
            osVersion = System.getProperty("os.version");
            Files.createDirectories(javaHome.resolve("lib"));
            Files.createDirectories(javaHome.resolve("conf").resolve("fonts"));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to set tmp java.home for FontConfig Quarkus AWT usage in " + javaHome, e);
        }
    }
}

/**
 * See Target_sun_awt_FontConfiguration_Linux, for context.
 *
 * Windows doesn't have fontconfig package and its config file installed like Linux has.
 * Java runtime looks for the config file inside our fake JAVA_HOME.
 * We provide a skeleton, i18n ignorant version to satisfy the basic headless fonts processing.
 */
@TargetClass(className = "sun.awt.FontConfiguration", onlyWith = IsWindows.class)
final class Target_sun_awt_FontConfiguration_Windows {
    @Alias
    protected static String osVersion;
    @Alias
    protected static String osName;

    @Substitute
    protected void setOsNameAndVersion() {
        final Path javaHome = Path.of(System.getProperty("java.io.tmpdir"), "quarkus-awt-tmp-fonts");
        try {
            System.setProperty("java.home", javaHome.toString());
            osName = System.getProperty("os.name", "unknown");
            osVersion = System.getProperty("os.version");
            Files.createDirectories(javaHome.resolve("lib"));
            Files.createDirectories(javaHome.resolve("conf").resolve("fonts"));
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to set tmp java.home for FontConfig Quarkus AWT usage in " + javaHome, e);
        }
        final Path configFile = javaHome.resolve("lib").resolve("fontconfig.properties");
        try {
            if (!Files.exists(configFile)) {
                // JAVA_HOME/lib/fontconfig.properties.src
                final String minimalConfig = "version=1\n" +
                        "sequence.allfonts=alphabetic\n" +
                        "allfonts.symbol=Symbol\n" +
                        "allfonts.symbols=Segoe UI Symbol\n" +
                        "serif.plain.alphabetic=Times New Roman\n" +
                        "serif.bold.alphabetic=Times New Roman Bold\n" +
                        "serif.italic.alphabetic=Times New Roman Italic\n" +
                        "serif.bolditalic.alphabetic=Times New Roman Bold Italic\n" +
                        "sansserif.plain.alphabetic=Arial\n" +
                        "sansserif.bold.alphabetic=Arial Bold\n" +
                        "sansserif.italic.alphabetic=Arial Italic\n" +
                        "sansserif.bolditalic.alphabetic=Arial Bold Italic\n" +
                        "monospaced.plain.alphabetic=Courier New\n" +
                        "monospaced.bold.alphabetic=Courier New Bold\n" +
                        "monospaced.italic.alphabetic=Courier New Italic\n" +
                        "monospaced.bolditalic.alphabetic=Courier New Bold Italic\n" +
                        "dialog.plain.alphabetic=Arial\n" +
                        "dialog.bold.alphabetic=Arial Bold\n" +
                        "dialog.italic.alphabetic=Arial Italic\n" +
                        "dialog.bolditalic.alphabetic=Arial Bold Italic\n" +
                        "dialoginput.plain.alphabetic=Courier New\n" +
                        "dialoginput.bold.alphabetic=Courier New Bold\n" +
                        "dialoginput.italic.alphabetic=Courier New Italic\n" +
                        "dialoginput.bolditalic.alphabetic=Courier New Bold Italic\n" +
                        // Windows is case insensitive, doesn't matter.
                        "filename.Arial=ARIAL.TTF\n" +
                        "filename.Arial_Bold=ARIALBD.TTF\n" +
                        "filename.Arial_Italic=ARIALI.TTF\n" +
                        "filename.Arial_Bold_Italic=ARIALBI.TTF\n" +
                        "filename.Courier_New=COUR.TTF\n" +
                        "filename.Courier_New_Bold=COURBD.TTF\n" +
                        "filename.Courier_New_Italic=COURI.TTF\n" +
                        "filename.Courier_New_Bold_Italic=COURBI.TTF\n" +
                        "filename.Times_New_Roman=TIMES.TTF\n" +
                        "filename.Times_New_Roman_Bold=TIMESBD.TTF\n" +
                        "filename.Times_New_Roman_Italic=TIMESI.TTF\n" +
                        "filename.Times_New_Roman_Bold_Italic=TIMESBI.TTF\n" +
                        "filename.Symbol=SYMBOL.TTF\n" +
                        "filename.Wingdings=WINGDING.TTF\n";
                Files.writeString(configFile, minimalConfig, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write Windows " + configFile.toAbsolutePath(), e);
        }
    }
}

/**
 * Cut the dependency on Swing and Printing - we support server side, headless mode.
 * TODO: If an extension in Quarkiverse complains, we revisit it here.
 */
@TargetClass(className = "sun.awt.windows.WToolkit", onlyWith = IsWindows.class)
final class Target_sun_awt_windows_WToolkit {

    @Substitute
    public PrintJob getPrintJob(Frame frame, String jobtitle, Properties props) {
        throw new UnsupportedOperationException("Printing is not supported with Quarkus AWT extension.");
    }

    @Substitute
    public PrintJob getPrintJob(Frame frame, String jobtitle, JobAttributes jobAttributes, PageAttributes pageAttributes) {
        throw new UnsupportedOperationException("Printing is not supported with Quarkus AWT extension.");
    }
}

/**
 * Cut ties to windowing, desktop
 * TODO: If an extension in Quarkiverse complains, we revisit it here.
 */
@TargetClass(className = "sun.awt.windows.WObjectPeer", onlyWith = IsWindows.class)
final class Target_sun_awt_windows_WObjectPeer {
    @Substitute
    private static void initIDs() {
        // no-op, no pData, destroyed, target
    }
}

/**
 * Cut ties to D3D, accelerated desktop, etc.
 * Forces software rendering.
 * TODO: If an extension in Quarkiverse complains, we revisit it here.
 */
@TargetClass(className = "sun.java2d.windows.WindowsFlags", onlyWith = IsWindows.class)
final class Target_sun_java2d_windows_WindowsFlags {
    @Substitute
    private static boolean initNativeFlags() {
        return false;
    }
}

/**
 * This is needed for JDK 21, it's not necessary for JDK 25.
 * We don't need to go down the Swing route initialization of popup menus
 * for server-side headless mode.
 * TODO: If an extension in Quarkiverse complains, we revisit it here.
 */
@TargetClass(className = "sun.awt.im.ExecutableInputMethodManager", onlyWith = { IsWindows.class, JavaVersionLessThan25.class })
final class Target_sun_awt_im_ExecutableInputMethodManager {
    @Substitute
    private void run() {
        // No-op
    }
}

/**
 * This is needed for JDK 21, it's not necessary for JDK 25.
 * We don't need to go down the Swing route initialization of the composition area
 * for server-side headless mode.
 * TODO: If an extension in Quarkiverse complains, we revisit it here.
 */
@TargetClass(className = "sun.awt.im.CompositionAreaHandler", onlyWith = { IsWindows.class, JavaVersionLessThan25.class })
final class Target_sun_awt_im_CompositionAreaHandler {
    @Substitute
    private void createCompositionArea() {
        // No-op
    }
}

public class JDKSubstitutions {
}
