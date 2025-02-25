package io.quarkus.vertx.http.deployment.webjar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.ResolvedDependency;
import io.quarkus.runtime.ApplicationConfig;

/**
 * Filter for inserting variables into an InputStream.
 * Supported placeholders are:
 * <ul>
 * <li>{applicationName}</li>
 * <li>{applicationVersion}</li>
 * <li>{quarkusVersion}</li>
 * </ul>
 */
public class InsertVariablesResourcesFilter implements WebJarResourcesFilter {

    private static final String CSS = ".css";

    private final ApplicationConfig applicationConfig;
    private final ResolvedDependency appArtifact;

    public InsertVariablesResourcesFilter(ApplicationConfig applicationConfig, ResolvedDependency appArtifact) {
        this.applicationConfig = applicationConfig;
        this.appArtifact = appArtifact;
    }

    @Override
    public FilterResult apply(String fileName, InputStream stream) throws IOException {
        if (stream == null) {
            return new FilterResult(null, false);
        }

        // Allow replacement of certain values in css
        if (fileName.endsWith(CSS)) {
            String applicationName = applicationConfig.name().orElse(appArtifact.getArtifactId());
            String applicationVersion = applicationConfig.version().orElse(appArtifact.getVersion());

            byte[] oldContentBytes = stream.readAllBytes();
            String oldContents = new String(oldContentBytes);
            String contents = replaceHeaderVars(oldContents, applicationName, applicationVersion);

            String header = replaceHeaderVars(applicationConfig.uiHeader().orElse(""), applicationName, applicationVersion);
            contents = contents.replace("{applicationHeader}", header);

            boolean changed = contents.length() != oldContents.length() || !contents.equals(oldContents);
            if (changed) {
                return new FilterResult(new ByteArrayInputStream(contents.getBytes()), true);
            } else {
                return new FilterResult(new ByteArrayInputStream(oldContentBytes), false);
            }
        }

        return new FilterResult(stream, false);
    }

    private static String replaceHeaderVars(String contents, String applicationName, String applicationVersion) {
        contents = contents.replace("{applicationName}", applicationName);
        contents = contents.replace("{applicationVersion}", applicationVersion);
        contents = contents.replace("{quarkusVersion}", Version.getVersion());
        return contents;
    }
}
