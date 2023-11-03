package io.quarkus.qute.runtime;

import java.net.URLConnection;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.qute.Variant;

@Singleton
public class ContentTypes {

    private static final Logger LOGGER = Logger.getLogger(ContentTypes.class);

    @Inject
    QuteConfig config;

    /**
     *
     * @param templatePath The path relative to the template root, uses the {@code /} path separator.
     * @return the content type
     */
    public String getContentType(String templatePath) {
        String fileName = templatePath;
        int slashIdx = fileName.lastIndexOf('/');
        if (slashIdx != -1) {
            fileName = fileName.substring(slashIdx, fileName.length());
        }
        int dotIdx = fileName.lastIndexOf('.');
        if (dotIdx != -1) {
            String suffix = fileName.substring(dotIdx + 1, fileName.length());
            String additionalContentType = config.contentTypes.get(suffix);
            if (additionalContentType != null) {
                return additionalContentType;
            }
            if (suffix.equalsIgnoreCase("json")) {
                return Variant.APPLICATION_JSON;
            }
            String contentType = URLConnection.getFileNameMap().getContentTypeFor(fileName);
            if (contentType != null) {
                return contentType;
            }
        }
        LOGGER.warn("Unable to detect the content type for " + templatePath + "; using application/octet-stream");
        return "application/octet-stream";
    }

}
