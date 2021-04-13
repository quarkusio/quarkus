package io.quarkus.micrometer.runtime.binder;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Timer;

public class RequestMetricInfo {
    static final Logger log = Logger.getLogger(RequestMetricInfo.class);

    public static final Pattern TRAILING_SLASH_PATTERN = Pattern.compile("/$");
    public static final Pattern MULTIPLE_SLASH_PATTERN = Pattern.compile("//+");
    public static final String ROOT = "/";

    public static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";

    /** Store the sample used to measure the request */
    protected Timer.Sample sample;

    public RequestMetricInfo setSample(Timer.Sample sample) {
        this.sample = sample;
        return this;
    }

    public Timer.Sample getSample() {
        return sample;
    }

    /**
     * Normalize and filter request path against match patterns
     *
     * @param uri Uri for request
     * @param ignorePatterns
     * @param matchPatterns
     * @return final uri for tag, or null to skip measurement
     */
    protected String getNormalizedUriPath(Map<Pattern, String> matchPatterns, List<Pattern> ignorePatterns, String uri) {
        // Normalize path
        String path = normalizePath(uri);
        if (path.length() > 1) {
            String origPath = path;
            // Look for configured matches, then inferred templates
            path = applyMatchPatterns(origPath, matchPatterns);
            if (path.equals(origPath)) {
                path = normalizePath(applyTemplateMatching(origPath));
            }
        }
        return filterIgnored(path, ignorePatterns);
    }

    /** Subclassess should override with appropriate mechanisms for finding templated urls */
    protected String applyTemplateMatching(String path) {
        return path;
    }

    static String applyMatchPatterns(String path, Map<Pattern, String> matchPatterns) {
        if (!matchPatterns.isEmpty()) {
            for (Map.Entry<Pattern, String> mp : matchPatterns.entrySet()) {
                if (mp.getKey().matcher(path).matches()) {
                    log.debugf("Path %s matched pattern %s, using %s", path, mp.getKey(), mp.getValue());
                    return mp.getValue();
                }
            }
        }
        return path;
    }

    /** Return path or null if it should be ignored */
    protected static String filterIgnored(String path, List<Pattern> ignorePatterns) {
        if (!ignorePatterns.isEmpty()) {
            for (Pattern p : ignorePatterns) {
                if (p.matcher(path).matches()) {
                    log.debugf("Path %s ignored; matches pattern %s", path, p.pattern());
                    return null;
                }
            }
        }
        return path;
    }

    protected static String normalizePath(String uri) {
        if (uri == null || uri.isEmpty() || ROOT.equals(uri)) {
            return ROOT;
        }
        // Label value consistency: result should begin with a '/' and should not end with one
        String workingPath = MULTIPLE_SLASH_PATTERN.matcher('/' + uri).replaceAll("/");
        workingPath = TRAILING_SLASH_PATTERN.matcher(workingPath).replaceAll("");
        if (workingPath.isEmpty()) {
            return ROOT;
        }
        return workingPath;
    }
}
