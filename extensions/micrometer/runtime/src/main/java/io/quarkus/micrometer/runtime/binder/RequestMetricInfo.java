package io.quarkus.micrometer.runtime.binder;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;

import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

public class RequestMetricInfo {
    static final Logger log = Logger.getLogger(RequestMetricInfo.class);

    public static final String HTTP_REQUEST_PATH = "HTTP_REQUEST_PATH";
    public static final String HTTP_REQUEST_PATH_MATCHED = "HTTP_REQUEST_MATCHED_PATH";

    /** Do not measure requests until/unless a uri path is set */
    protected final boolean measure;

    /** URI path used as a tag value for non-error requests */
    protected final String path;

    /** True IFF the path was revised by a matcher expression */
    protected final boolean pathMatched;

    /** Store the sample used to measure the request */
    protected Timer.Sample sample;

    /**
     * Store the tags associated with the request (change 1.6.0).
     * Default is empty, value assigned @ requestBegin
     */
    protected Tags tags = Tags.empty();

    /**
     * Extract the path out of the uri. Return null if the path should be
     * ignored.
     */
    public RequestMetricInfo(Map<Pattern, String> matchPattern, List<Pattern> ignorePatterns,
            String uri) {
        if (uri == null) {
            this.measure = false;
            this.pathMatched = false;
            this.path = null;
            return;
        }

        boolean matched = false;
        String workingPath = extractPath(uri);
        String finalPath = workingPath;
        if ("/".equals(workingPath) || workingPath.isEmpty()) {
            finalPath = "/";
        } else {
            // Label value consistency: result should begin with a '/' and should not end with one
            workingPath = HttpCommonTags.MULTIPLE_SLASH_PATTERN.matcher('/' + workingPath).replaceAll("/");
            workingPath = HttpCommonTags.TRAILING_SLASH_PATTERN.matcher(workingPath).replaceAll("");
            if (workingPath.isEmpty()) {
                finalPath = "/";
            } else {
                finalPath = workingPath;
                // test path against configured patterns (whole path)
                for (Map.Entry<Pattern, String> mp : matchPattern.entrySet()) {
                    if (mp.getKey().matcher(workingPath).matches()) {
                        finalPath = mp.getValue();
                        matched = true;
                        break;
                    }
                }
            }
        }
        this.path = finalPath;
        this.pathMatched = matched;

        // Compare path against "ignore this path" patterns
        for (Pattern p : ignorePatterns) {
            if (p.matcher(this.path).matches()) {
                log.debugf("Path %s ignored; matches pattern %s", uri, p.pattern());
                this.measure = false;
                return;
            }
        }
        this.measure = true;
    }

    public Timer.Sample getSample() {
        return sample;
    }

    public void setSample(Timer.Sample sample) {
        this.sample = sample;
    }

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    public String getPath() {
        return path;
    }

    public boolean isMeasure() {
        return measure;
    }

    public boolean isPathMatched() {
        return pathMatched;
    }

    private static String extractPath(String uri) {
        if (uri.isEmpty()) {
            return uri;
        }
        int i;
        if (uri.charAt(0) == '/') {
            i = 0;
        } else {
            i = uri.indexOf("://");
            if (i == -1) {
                i = 0;
            } else {
                i = uri.indexOf('/', i + 3);
                if (i == -1) {
                    // contains no /
                    return "/";
                }
            }
        }

        int queryStart = uri.indexOf('?', i);
        if (queryStart == -1) {
            queryStart = uri.length();
        }
        return uri.substring(i, queryStart);
    }

    public String getHttpRequestPath() {
        return path;
    }

    @Override
    public String toString() {
        return "HttpRequestMetric{path=" + path
                + ",pathMatched=" + pathMatched
                + ",measure=" + measure
                + ",tags=" + tags
                + '}';
    }
}
