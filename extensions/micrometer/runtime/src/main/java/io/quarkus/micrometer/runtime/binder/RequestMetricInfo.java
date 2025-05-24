package io.quarkus.micrometer.runtime.binder;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Timer;

public class RequestMetricInfo {
    static final Logger log = Logger.getLogger(RequestMetricInfo.class);

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

    /** Subclasses should override with appropriate mechanisms for finding templated urls */
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

        String workingPath = new String(uri);

        // Remove all leading slashes
        // detect
        int start = 0;
        while (start < workingPath.length() && workingPath.charAt(start) == '/') {
            start++;
        }
        // Add missing / and remove multiple leading
        if (start != 1) {
            workingPath = "/" + workingPath.substring(start);
        }

        // Collapse multiple trailing slashes
        int end = workingPath.length();
        while (end > 1 && workingPath.charAt(end - 1) == '/') {
            end--;
        }

        if (end != workingPath.length()) {
            workingPath = workingPath.substring(0, end);
        }

        return workingPath;
    }
}
