package io.quarkus.awt.runtime;

import java.awt.AWTError;
import java.awt.FontFormatException;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.JobAttributes;
import java.awt.PageAttributes;
import java.awt.PrintJob;
import java.awt.Toolkit;
import java.awt.print.PrinterJob;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Properties;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.quarkus.runtime.util.IsLinux;
import io.quarkus.runtime.util.IsMac;
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
 * See Target_sun_awt_FontConfiguration_Linux, for context.
 *
 * On macOS, sun.font.CFontConfiguration uses CoreText for fonts, but FontConfiguration#init()
 * still needs a config file, "Fontconfig head is null" otherwise. The JDK has JAVA_HOME/lib/fontconfig.bfc.
 * We provide a minimal one, CFontConfiguration overrides all the lookups.
 */
@TargetClass(className = "sun.awt.FontConfiguration", onlyWith = IsMac.class)
final class Target_sun_awt_FontConfiguration_Mac {
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
        final byte[] minimalConfig = "version=1\nsequence.allfonts=default\n".getBytes(StandardCharsets.UTF_8);
        boolean upToDate;
        try {
            upToDate = Arrays.equals(minimalConfig, Files.readAllBytes(configFile));
        } catch (IOException e) {
            // missing or unreadable
            upToDate = false;
        }
        if (!upToDate) {
            // All native executables of the user share the file. Move it in place, so that a concurrent
            // process never reads a partial file, and a partial file of a killed process gets replaced,
            // "Fontconfig head is null" or a NullPointerException in FontConfiguration otherwise.
            final Path tmpFile = configFile.resolveSibling(configFile.getFileName() + "." + ProcessHandle.current().pid());
            try {
                Files.write(tmpFile, minimalConfig);
                Files.move(tmpFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                // e.g. a read-only directory with the file of another Quarkus version, which works too
                if (!Files.isReadable(configFile)) {
                    throw new UncheckedIOException("Failed to write macOS " + configFile.toAbsolutePath(), e);
                }
            } finally {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (IOException e) {
                    // ignore, the file is only left over after a failed move
                }
            }
        }
    }
}

final class MacHeadless {
    static final String HEADFUL_NOT_SUPPORTED = "Only headless AWT is supported in a native executable on macOS "
            + "(AppKit needs the main thread run loop). Do not set -Djava.awt.headless=false.";

    private MacHeadless() {
    }
}

/**
 * On macOS, AWT is headful by default whenever a WindowServer (Aqua) session is available,
 * i.e. on any desktop. The headful path initializes AppKit, which needs the main thread
 * run loop, and that is not supported in a native executable. We support server side,
 * headless mode only, -Djava.awt.headless=false fails fast with an AWTError.
 *
 * createToolkit returns the HeadlessToolkit wrapper directly, so Toolkit.getDefaultToolkit() never
 * stores the bare LWCToolkit in its static field. Otherwise, the points-to analysis resolves Toolkit
 * calls to the LWToolkit/LWCToolkit peer factories, i.e. Swing backed sun.lwawt.LW*Peer peers, AppKit
 * windows, menus, tray, desktop, robot, DnD, clipboard and input methods. At run time, it is the same
 * as a headless JVM. LWCToolkit is still needed behind HeadlessToolkit for images, fonts and desktop
 * properties, and its static initializer loads libawt and libfontmanager.
 */
@TargetClass(className = "sun.awt.PlatformGraphicsInfo", onlyWith = IsMac.class)
final class Target_sun_awt_PlatformGraphicsInfo_Mac {
    @Substitute
    public static boolean getDefaultHeadlessProperty() {
        return true;
    }

    /**
     * Appended to every HeadlessException, e.g. new Frame(), when java.awt.headless is not set.
     */
    @Substitute
    public static String getDefaultHeadlessMessage() {
        return "\nAWT is headless in a native executable on macOS,\n"
                + "but this program performed an operation which requires a display, keyboard, or mouse.";
    }

    @Substitute
    public static Toolkit createToolkit() {
        if (!GraphicsEnvironment.isHeadless()) {
            throw new AWTError(MacHeadless.HEADFUL_NOT_SUPPORTED);
        }
        final Toolkit lwcToolkit = (Toolkit) (Object) new Target_sun_lwawt_macosx_LWCToolkit();
        return (Toolkit) (Object) new Target_sun_awt_HeadlessToolkit(lwcToolkit);
    }
}

@TargetClass(className = "sun.awt.HeadlessToolkit", onlyWith = IsMac.class)
final class Target_sun_awt_HeadlessToolkit {
    @Alias
    Target_sun_awt_HeadlessToolkit(Toolkit tk) {
    }
}

/**
 * The macOS toolkit, only ever used behind HeadlessToolkit, see Target_sun_awt_PlatformGraphicsInfo_Mac.
 */
@TargetClass(className = "sun.lwawt.macosx.LWCToolkit", onlyWith = IsMac.class)
final class Target_sun_lwawt_macosx_LWCToolkit {
    @Alias
    Target_sun_lwawt_macosx_LWCToolkit() {
    }

    /**
     * Do not start AppKit. In headless mode, the native code only queues AWTStarter on the main
     * thread run loop, nobody runs that loop in a native executable. If it ran, it would rename
     * the thread to "AppKit Thread" and null its context class loader (installToolkitThreadInJava).
     */
    @Substitute
    private static void initAppkit(ThreadGroup appKitThreadGroup, boolean headless) {
        if (!headless) {
            throw new AWTError(MacHeadless.HEADFUL_NOT_SUPPORTED);
        }
    }

    /**
     * Reached from Toolkit.getDesktopProperty(...) via HeadlessToolkit. The native implementation
     * waits for the main thread run loop, i.e. it would block forever. 500 ms is the usual default.
     */
    @Substitute
    private static int getMultiClickTime() {
        return 500;
    }

    /**
     * No "NSImage://name" AppKit named images, the name is treated as a file name like on other platforms.
     */
    @Substitute
    private Image checkForNSImage(String imageName) {
        return null;
    }
}

/**
 * Cut ties to screen devices, i.e. CGraphicsDevice and the Metal and OpenGL pipelines.
 * Only called when headful, see Target_sun_awt_PlatformGraphicsInfo_Mac.
 */
@TargetClass(className = "sun.awt.CGraphicsEnvironment", onlyWith = IsMac.class)
final class Target_sun_awt_CGraphicsEnvironment {
    @Substitute
    private void rebuildDevices() {
        throw new AWTError(MacHeadless.HEADFUL_NOT_SUPPORTED);
    }
}

/**
 * Cut the dependency on printing on macOS: sun.lwawt.macosx.CPrinterJob, AppKit NSPrintInfo, print dialogs.
 * We support server side, headless mode.
 * TODO: If an extension in Quarkiverse complains, we revisit it here.
 */
@TargetClass(className = "sun.print.PlatformPrinterJobProxy", onlyWith = IsMac.class)
final class Target_sun_print_PlatformPrinterJobProxy {
    @Substitute
    public static PrinterJob getPrinterJob() {
        throw new UnsupportedOperationException("Printing is not supported with Quarkus AWT extension.");
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
