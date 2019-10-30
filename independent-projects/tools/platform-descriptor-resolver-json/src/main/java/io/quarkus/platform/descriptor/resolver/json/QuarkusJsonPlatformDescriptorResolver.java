package io.quarkus.platform.descriptor.resolver.json;

import static io.quarkus.platform.tools.ToolsUtils.getProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.apache.maven.model.Dependency;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.json.ArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;
import io.quarkus.platform.tools.DefaultMessageWriter;
import io.quarkus.platform.tools.MessageWriter;
import io.quarkus.platform.tools.ToolsConstants;

/**
 * Helps resolve the specific or the latest available version of a JSON platform descriptor.
 */
public class QuarkusJsonPlatformDescriptorResolver {

    public static final String PROP_PLATFORM_JSON_GROUP_ID = "quarkus.platform.json.groupId";
    public static final String PROP_PLATFORM_JSON_ARTIFACT_ID = "quarkus.platform.json.artifactId";
    public static final String PROP_PLATFORM_JSON_VERSION = "quarkus.platform.json.version";
    public static final String PROP_PLATFORM_JSON_VERSION_RANGE = "quarkus.platform.json.version-range";

    public static QuarkusJsonPlatformDescriptorResolver newInstance() {
        return new QuarkusJsonPlatformDescriptorResolver();
    }

    private String jsonGroupId;
    private String jsonArtifactId;
    private String jsonVersionRange;
    private String jsonVersion;

    private String bomGroupId;
    private String bomArtifactId;
    private String bomVersion;

    private AppModelResolver artifactResolver;
    private MessageWriter log;

    public QuarkusJsonPlatformDescriptorResolver() {
    }

    public QuarkusJsonPlatformDescriptorResolver setJsonVersion(String groupId, String artifactId, String version) {
        this.jsonGroupId = groupId;
        this.jsonArtifactId = artifactId;
        this.jsonVersion = version;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setJsonVersionRange(String groupId, String artifactId, String versionRange) {
        this.jsonGroupId = groupId;
        this.jsonArtifactId = artifactId;
        this.jsonVersionRange = versionRange;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setJsonArtifactId(String artifactId) {
        this.jsonArtifactId = artifactId;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setBomVersion(String groupId, String artifactId, String version) {
        this.bomGroupId = groupId;
        this.bomArtifactId = artifactId;
        this.bomVersion = version;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setArtifactResolver(AppModelResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setMessageWriter(MessageWriter msgWriter) {
        this.log = msgWriter;
        return this;
    }

    public QuarkusPlatformDescriptor resolve() {

        if(log == null) {
            log = new DefaultMessageWriter();
        }

        AppModelResolver artifactResolver = this.artifactResolver;
        if(artifactResolver == null) {
            try {
                artifactResolver = new BootstrapAppModelResolver(MavenArtifactResolver.builder().build());
            } catch (AppModelResolverException e) {
                throw new IllegalStateException("Failed to initialize the Maven artifact resolver", e);
            }
        }

        String jsonGroupId = this.jsonGroupId;
        String jsonArtifactId = this.jsonArtifactId;
        String jsonVersion = this.jsonVersion;
        AppArtifact jsonArtifact;
        if(bomArtifactId != null) {
            String bomGroupId = this.bomGroupId;
            if(bomGroupId == null) {
                bomGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
            }
            String bomArtifactId = this.bomArtifactId;
            String bomVersion = this.bomVersion;
            if(bomVersion == null) {
                throw new IllegalStateException("Quarkus Platform BOM version is missing");
            }
            // first we are looking for the same GAV as the BOM but with JSON type
            jsonArtifact = new AppArtifact(bomGroupId, bomArtifactId, null, "json", bomVersion);
            try {
                artifactResolver.resolve(jsonArtifact);
            } catch (Throwable e) {
                // it didn't work, now we are trying artifactId-descriptor-json
                jsonArtifact = new AppArtifact(bomGroupId, bomArtifactId + "-descriptor-json", null, "json", bomVersion);
                try {
                    artifactResolver.resolve(jsonArtifact);
                } catch (Throwable e1) {
                    throw new IllegalStateException("Failed to determine the coordinates of the JSON descriptor artifact for " + bomGroupId + ":" + bomArtifactId + ":" + bomVersion);
                }
            }
        } else {
            if (jsonGroupId == null) {
                jsonGroupId = getProperty(PROP_PLATFORM_JSON_GROUP_ID, ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID);
            }
            if (jsonArtifactId == null) {
                jsonArtifactId = getProperty(PROP_PLATFORM_JSON_ARTIFACT_ID, ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID);
            }
            if (jsonVersion == null) {
                if (jsonVersionRange != null) {
                    // if the range was set using the api, it overrides a possibly set version system property
                    // depending on how this evolves this may or may not be reasonable
                    jsonVersion = resolveLatestJsonVersion(artifactResolver, jsonGroupId, jsonArtifactId, jsonVersionRange);
                } else {
                    jsonVersion = getProperty(PROP_PLATFORM_JSON_VERSION);
                    if (jsonVersion == null) {
                        jsonVersion = resolveLatestJsonVersion(artifactResolver, jsonGroupId, jsonArtifactId, jsonVersionRange);
                    }
                }
            }
            jsonArtifact = new AppArtifact(jsonGroupId, jsonArtifactId, null, "json", jsonVersion);
        }

        // Resolve the platform JSON artifact
        log.debug("Platform JSON artifact: %s", jsonArtifact);
        final Path jsonFile;
        try {
            jsonFile = artifactResolver.resolve(jsonArtifact);
        } catch (AppModelResolverException e) {
            throw new IllegalStateException("Failed to resolve the platform json artifact " + jsonArtifact, e);
        }
        if(!Files.exists(jsonFile)) {
            throw new IllegalStateException("Failed to locate extensions JSON file at " + jsonFile);
        }

        // Resolve the Quarkus version used by the platform
        final String quarkusCoreVersion;
        try(BufferedReader reader = Files.newBufferedReader(jsonFile)) {
            JsonObject jsonObject = Json.parse(reader).asObject();
            quarkusCoreVersion = jsonObject.getString("quarkus-core-version", null);
            if(quarkusCoreVersion == null) {
                throw new IllegalStateException("Failed to determine the Quarkus Core version for " + jsonFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse extensions JSON file " + jsonFile);
        }
        log.debug("Quarkus version: %s", quarkusCoreVersion);

        // Resolve the JSON platform descriptor loader from the target Quarkus release
        return loadPlatformDescriptor(artifactResolver, jsonFile, quarkusCoreVersion);
    }

    @SuppressWarnings("rawtypes")
    private QuarkusPlatformDescriptor loadPlatformDescriptor(AppModelResolver mvn, final Path jsonFile,
            String quarkusCoreVersion) {
        final AppArtifact jsonDescrArtifact = new AppArtifact(ToolsConstants.IO_QUARKUS, "quarkus-platform-descriptor-json", null, "jar", quarkusCoreVersion);
        final URL jsonDescrUrl;
        try {
            jsonDescrUrl = mvn.resolve(jsonDescrArtifact).toUri().toURL();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to resolve " + jsonDescrArtifact, e);
        }

        final URLClassLoader jsonDescrLoaderCl = new URLClassLoader(new URL[] {jsonDescrUrl}, Thread.currentThread().getContextClassLoader());
        final QuarkusPlatformDescriptor platform;
        try {
            final Iterator<QuarkusJsonPlatformDescriptorLoader> i = ServiceLoader.load(QuarkusJsonPlatformDescriptorLoader.class, jsonDescrLoaderCl).iterator();
            if(!i.hasNext()) {
                throw new IllegalStateException("Failed to locate an implementation of " + QuarkusJsonPlatformDescriptorLoader.class.getName());
            }
            final QuarkusJsonPlatformDescriptorLoader<?> jsonDescrLoader = i.next();
            if(i.hasNext()) {
                throw new IllegalStateException("Located more than one implementation of " + QuarkusJsonPlatformDescriptorLoader.class.getName());
            }
            log.debug("Using JSON platform descriptor loader %s", jsonDescrLoader);
            final ArtifactResolver loaderResolver = new ArtifactResolver() {

                @Override
                public <T> T process(String groupId, String artifactId, String classifier, String type, String version,
                        Function<Path, T> processor) {
                    final AppArtifact artifact = new AppArtifact(groupId, artifactId, classifier, type, version);
                    try {
                        return processor.apply(artifactResolver.resolve(artifact));
                    } catch (AppModelResolverException e) {
                        throw new IllegalStateException("Failed to resolve " + artifact, e);
                    }
                }

                @Override
                public List<Dependency> getManagedDependencies(String groupId, String artifactId, String classifier,
                        String type, String version) {
                    if(!"pom".equals(type)) {
                        throw new IllegalStateException("This implementation expects artifacts of type pom");
                    }
                    final Path pom;
                    final AppArtifact pomArtifact = new AppArtifact(groupId, artifactId, classifier, type, version);
                    try {
                        pom = artifactResolver.resolve(pomArtifact);
                    } catch (AppModelResolverException e) {
                        throw new IllegalStateException("Failed to resolve " + pomArtifact, e);
                    }
                    try {
                        return ModelUtils.readModel(pom).getDependencyManagement().getDependencies();
                    } catch (IOException e) {
                        throw new IllegalStateException("Failed to read model of " + pom, e);
                    }
                }};

            platform = jsonDescrLoader.load(new QuarkusJsonPlatformDescriptorLoaderContext() {
                @Override
                public <T> T parseJson(Function<Path, T> parser) {
                    return parser.apply(jsonFile);
                }

                @Override
                public MessageWriter getMessageWriter() {
                    return log;
                }

                @Override
                public ArtifactResolver getArtifactResolver() {
                    return loaderResolver;
                }});
        } finally {
            try {
                jsonDescrLoaderCl.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return platform;
    }

    private String resolveLatestJsonVersion(AppModelResolver artifactResolver, String groupId, String artifactId, String versionRange) {
        if(versionRange == null) {
            versionRange = getProperty(PROP_PLATFORM_JSON_VERSION_RANGE);
            if(versionRange == null) {
                versionRange = "[0,)";
            }
        }
        try {
            return resolveLatestFromVersionRange(artifactResolver, groupId, artifactId, null, "json", versionRange);
        } catch (AppModelResolverException e) {
            throw new IllegalStateException("Failed to resolve the latest JSON platform version of " + groupId + ":" + artifactId + "::json:" + versionRange);
        }
    }

    private String resolveLatestFromVersionRange(AppModelResolver mvn, String groupId, String artifactId, String classifier, String type, final String versionRange)
            throws AppModelResolverException {
        final AppArtifact appArtifact = new AppArtifact(groupId, artifactId, classifier, type, versionRange);
        log.debug("Resolving the latest version of " + appArtifact);
        final String latestVersion = mvn.getLatestVersionFromRange(appArtifact, versionRange);
        if(latestVersion == null) {
            throw new IllegalStateException("Failed to resolve the latest version of " + appArtifact);
        }
        return latestVersion;
    }
}
