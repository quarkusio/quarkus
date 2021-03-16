package io.quarkus.vertx.http.runtime;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class that can check known user agents which are known to be incompatible with SameSite=None attribute.
 * <p>
 * <ul>
 * <li>Versions of Chrome from Chrome 51 to Chrome 66 (inclusive on both ends).
 * These Chrome versions will reject a cookie with `SameSite=None`. This also
 * affects older versions of Chromium-derived browsers, as well as Android WebView.
 * This behavior was correct according to the version of the cookie specification
 * at that time, but with the addition of the new "None" value to the specification,
 * this behavior has been updated in Chrome 67 and newer. (Prior to Chrome 51,
 * the SameSite attribute was ignored entirely and all cookies were treated as if
 * they were `SameSite=None`.)</li>
 * <li>Versions of UC Browser on Android prior to version 12.13.2. Older versions
 * will reject a cookie with `SameSite=None`. This behavior was correct according
 * to the version of the cookie specification at that time, but with the addition of
 * the new "None" value to the specification, this behavior has been updated in newer
 * versions of UC Browser.
 * <li>Versions of Safari and embedded browsers on MacOS 10.14 and all browsers on iOS 12.
 * These versions will erroneously treat cookies marked with `SameSite=None` as if they
 * were marked `SameSite=Strict`. This bug has been fixed on newer versions of iOS and MacOS.
 * </ul>
 * <p>
 * 
 * @see <a href="https://www.chromium.org/updates/same-site/incompatible-clients">SameSite=None: Known Incompatible Clients</a>.
 */
final class SameSiteNoneIncompatibleClientChecker {

    /**
     * User Agents Regex Patterns
     */
    private static final Pattern IOS_PATTERN = Pattern.compile("\\(iP.+; CPU .*OS (\\d+)[_\\d]*.*\\) AppleWebKit\\/");
    private static final Pattern MACOSX_PATTERN = Pattern
            .compile("\\(Macintosh;.*Mac OS X (\\d+)_(\\d+)[_\\d]*.*\\) AppleWebKit\\/");
    private static final Pattern SAFARI_PATTERN = Pattern.compile("Version\\/.* Safari\\/");
    private static final Pattern MAC_EMBEDDED_BROWSER_PATTERN = Pattern.compile(
            "^Mozilla\\/[\\.\\d]+ \\(Macintosh;.*Mac OS X [_\\d]+\\) AppleWebKit\\/[\\.\\d]+ \\(KHTML, like Gecko\\)$");
    private static final Pattern CHROMIUM_PATTERN = Pattern.compile("Chrom(e|ium)");
    private static final Pattern CHROMIUM_VERSION_PATTERN = Pattern.compile("Chrom[^ \\/]+\\/(\\d+)[\\.\\d]* ");
    // private static final Pattern UC_BROWSER_PATTERN = Pattern.compile("UCBrowser\\/");
    private static final Pattern UC_BROWSER_VERSION_PATTERN = Pattern.compile("UCBrowser\\/(\\d+)\\.(\\d+)\\.(\\d+)[\\.\\d]* ");

    public static boolean shouldSendSameSiteNone(String useragent) {
        return !isSameSiteNoneIncompatible(useragent);
    }

    // browsers known to be incompatible.
    public static boolean isSameSiteNoneIncompatible(String useragent) {
        return hasWebKitSameSiteBug(useragent) ||
                dropsUnrecognizedSameSiteCookies(useragent);
    }

    static boolean hasWebKitSameSiteBug(String useragent) {
        return isIosVersion(12, useragent) ||
                (isMacosxVersion(10, 14, useragent) &&
                        (isSafari(useragent) || isMacEmbeddedBrowser(useragent)));
    }

    static boolean dropsUnrecognizedSameSiteCookies(String useragent) {
        if (isUcBrowser(useragent)) {
            return !isUcBrowserVersionAtLeast(12, 13, 2, useragent);
        }
        return isChromiumBased(useragent) &&
                isChromiumVersionAtLeast(51, useragent) &&
                !isChromiumVersionAtLeast(67, useragent);
    }

    // Regex parsing of User-Agent String. (See note above!)

    static boolean isIosVersion(int major, String useragent) {
        Matcher m = IOS_PATTERN.matcher(useragent);
        if (m.find()) {
            // Extract digits from first capturing group.
            return String.valueOf(major).equals(m.group(1));
        }
        return false;
    }

    static boolean isMacosxVersion(int major, int minor, String useragent) {
        Matcher m = MACOSX_PATTERN.matcher(useragent);
        if (m.find()) {
            // Extract digits from first and second capturing groups.
            return String.valueOf(major).equals(m.group(1)) &&
                    String.valueOf(minor).equals(m.group(2));
        }
        return false;
    }

    static boolean isSafari(String useragent) {
        return SAFARI_PATTERN.matcher(useragent).find() &&
                !isChromiumBased(useragent);
    }

    static boolean isMacEmbeddedBrowser(String useragent) {
        return MAC_EMBEDDED_BROWSER_PATTERN.matcher(useragent).find();
    }

    static boolean isChromiumBased(String useragent) {
        return CHROMIUM_PATTERN.matcher(useragent).find();
    }

    static boolean isChromiumVersionAtLeast(int major, String useragent) {
        Matcher m = CHROMIUM_VERSION_PATTERN.matcher(useragent);
        if (m.find()) {
            // Extract digits from first capturing group.
            int version = Integer.parseInt(m.group(1));
            return version >= major;
        }
        return false;
    }

    static boolean isUcBrowser(String useragent) {
        return useragent.contains("UCBrowser/");
    }

    static boolean isUcBrowserVersionAtLeast(int major, int minor, int build, String useragent) {
        Matcher m = UC_BROWSER_VERSION_PATTERN.matcher(useragent);
        if (m.find()) {
            // Extract digits from three capturing groups.
            int major_version = Integer.parseInt(m.group(1));
            int minor_version = Integer.parseInt(m.group(2));
            int build_version = Integer.parseInt(m.group(3));
            if (major_version != major) {
                return major_version > major;
            }
            if (minor_version != minor) {
                return minor_version > minor;
            }
            return build_version >= build;
        }
        return false;
    }

}
