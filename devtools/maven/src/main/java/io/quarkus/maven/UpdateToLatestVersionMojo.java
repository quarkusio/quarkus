package io.quarkus.maven;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Updates the Quarkus version properties to the latest version. The latest
 * version is retrieved from the release page on Github.
 */
@Mojo(name = "update-to-latest")
public class UpdateToLatestVersionMojo extends AbstractMojo {

    private static final String QUARKUS_PLATFORM_VERSION = "quarkus.platform.version";
    private static final String QUARKUS_PLUGIN_VERSION = "quarkus-plugin.version";

    // @Parameter(defaultValue = "${project}", readonly = false, required = true)
    // private MavenProject project;

    @Parameter(property = QUARKUS_PLATFORM_VERSION, required = true)
    private String platformVersion;

    @Parameter(property = QUARKUS_PLUGIN_VERSION, required = true)
    private String pluginVersion;

    @Parameter(property = "community", defaultValue = "true")
    private boolean isCommunity;

    private String pomfile = "pom.xml";
    private File pomFile = new File(pomfile);

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        MavenXpp3Reader mavenreader = new MavenXpp3Reader();
        Model model;
        try {
            FileReader reader = new FileReader(pomFile);
            model = mavenreader.read(reader);
            model.setPomFile(pomFile);
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage());
        }
        MavenProject project = new MavenProject(model);

        QuarkusVersion version = getInstance();
        HttpUriRequest request = new HttpGet(version.getSiteURL());

        String latestVersion = "";
        try {
            HttpResponse httpResponse = HttpClientBuilder.create().build().execute(request);
            latestVersion = version.getLatest(httpResponse.getEntity());
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        String oldPlatformVersion = (String) project.getProperties().setProperty(QUARKUS_PLATFORM_VERSION,
                latestVersion);
        getLog().info("Updated platform version from " + oldPlatformVersion + " to " + latestVersion);

        String oldPluginVersion = (String) project.getProperties().setProperty(QUARKUS_PLUGIN_VERSION, latestVersion);
        getLog().info("Updated plugin version from " + oldPluginVersion + " to " + latestVersion);

        try {
            MavenXpp3Writer writer = new MavenXpp3Writer();
            writer.write(new FileWriter(pomfile), model);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }

    }

    private QuarkusVersion getInstance() {
        if (isCommunity) {
            return new QuarkusVersion() {
                @Override
                public String getSiteURL() {
                    return "https://api.github.com/repos/quarkusio/quarkus/tags?per_page=1";
                }

                @Override
                public String getLatest(HttpEntity entity) throws IOException {
                    try (JsonReader jsonReader = Json.createReader(new StringReader(EntityUtils.toString(entity)))) {
                        JsonObject object = jsonReader.readArray().getJsonObject(0);
                        return object.getString("name");
                    }
                }
            };
        } else {
            return new QuarkusVersion() {
                @Override
                public String getSiteURL() {
                    return null;
                }

                public String getLatest(HttpEntity entity) throws IOException {
                    return null;
                }
            };
        }
    }
}

interface QuarkusVersion {
    String getSiteURL();

    String getLatest(HttpEntity entity) throws IOException;
}
