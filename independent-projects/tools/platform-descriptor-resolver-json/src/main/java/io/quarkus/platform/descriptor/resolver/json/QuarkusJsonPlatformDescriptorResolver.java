package io.quarkus.platform.descriptor.resolver.json;

import static io.quarkus.platform.tools.ToolsUtils.getProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.VersionRangeResult;
import org.eclipse.aether.version.Version;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.eclipsesource.json.WriterConfig;

import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.maven.utilities.MojoUtils;
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
    private MavenArtifactResolver mvn;
    private MessageWriter log;

    public QuarkusJsonPlatformDescriptorResolver() {
    }

    public QuarkusJsonPlatformDescriptorResolver setPlatformJsonGroupId(String groupId) {
        this.jsonGroupId = groupId;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setPlatformJsonArtifactId(String artifactId) {
        this.jsonArtifactId = artifactId;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setPlatformJsonVersionRange(String versionRange) {
        this.jsonVersionRange = versionRange;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setPlatformJsonVersion(String version) {
        this.jsonVersion = version;
        return this;
    }

    public QuarkusJsonPlatformDescriptorResolver setMavenArtifactResolver(MavenArtifactResolver mvn) {
        this.mvn = mvn;
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

        MavenArtifactResolver mvn = this.mvn;
        if(mvn == null) {
            try {
                mvn = MavenArtifactResolver.builder().build();
            } catch (AppModelResolverException e) {
                throw new IllegalStateException("Failed to initialize the Maven artifact resolver", e);
            }
        }

        String jsonGroupId = this.jsonGroupId;
        if(jsonGroupId == null) {
            jsonGroupId = getProperty(PROP_PLATFORM_JSON_GROUP_ID, ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID);
        }
        String jsonArtifactId = this.jsonArtifactId;
        if (jsonArtifactId == null) {
            jsonArtifactId = getProperty(PROP_PLATFORM_JSON_ARTIFACT_ID, ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID);
        }
        String jsonVersion = this.jsonVersion;
        if(jsonVersion == null) {
            if(jsonVersionRange != null) {
                // if the range was set using the api, it overrides a possibly set version system property
                // depending on how this evolves this may or may not be reasonable
                jsonVersion = resolveLatestJsonVersion(mvn, jsonGroupId, jsonArtifactId, jsonVersionRange);
            } else {
                jsonVersion = getProperty(PROP_PLATFORM_JSON_VERSION);
                if (jsonVersion == null) {
                    jsonVersion = resolveLatestJsonVersion(mvn, jsonGroupId, jsonArtifactId, jsonVersionRange);
                }
            }
        }

        // Resolve the platform JSON artifact
        Artifact jsonArtifact = new DefaultArtifact(jsonGroupId, jsonArtifactId, null, "json", jsonVersion);
        log.debug("Platform JSON artifact: %s", jsonArtifact);
        final File jsonFile;
        try {
            jsonFile = mvn.resolve(jsonArtifact).getArtifact().getFile();
        } catch (AppModelResolverException e) {
            throw new IllegalStateException("Failed to resolve the platform json artifact " + jsonArtifact, e);
        }
        if(!jsonFile.exists()) {
            throw new IllegalStateException("Failed to locate extensions JSON file at " + jsonFile);
        }

        // Resolve the platform BOM coordinates
        final String platformBomGroupId;
        final String platformBomArtifactId;
        final String platformBomVersion;
        try(BufferedReader reader = Files.newBufferedReader(jsonFile.toPath())) {
            JsonObject jsonObject = Json.parse(reader).asObject();
            JsonValue value = jsonObject.get("bom");
            if(value == null) {
                throw new IllegalStateException("Failed to determine the platform BOM behind " + jsonFile);
            }
            jsonObject = value.asObject();
            platformBomGroupId = resolveRequired(jsonObject, "groupId");
            platformBomArtifactId = resolveRequired(jsonObject, "artifactId");
            platformBomVersion = resolveRequired(jsonObject, "version");
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalStateException("Failed to parse extensions JSON file " + jsonFile);
        }
        log.debug("Platform BOM: %s:%s:%s", platformBomGroupId, platformBomArtifactId, platformBomVersion);

        // Resolve the Quarkus version used by the platform
        final ArtifactDescriptorResult platformDescr;
        final String quarkusCoreVersion;
        try {
            platformDescr = mvn.resolveDescriptor(new DefaultArtifact(platformBomGroupId, platformBomArtifactId, null, "pom", platformBomVersion));
            quarkusCoreVersion = resolveQuarkusCoreVersion(platformDescr);
        } catch (AppModelResolverException e) {
            throw new IllegalStateException("Failed to resolve the Quarkus Core version for the target Quarkus Platform", e);
        }
        log.debug("Quarkus Core Version: %s", quarkusCoreVersion);

        // Resolve the JSON platform descriptor loader from the target Quarkus release
        return loadPlatformDescriptor(mvn, jsonFile, quarkusCoreVersion, platformDescr);
    }

    @SuppressWarnings("rawtypes")
    private QuarkusPlatformDescriptor loadPlatformDescriptor(MavenArtifactResolver mvn, final File jsonFile,
            String quarkusCoreVersion, ArtifactDescriptorResult platformDescr) {
        final DefaultArtifact jsonDescrArtifact = new DefaultArtifact(ToolsConstants.IO_QUARKUS, "quarkus-platform-descriptor-json", null, "jar", quarkusCoreVersion);
        final URL jsonDescrUrl;
        try {
            jsonDescrUrl = mvn.resolve(jsonDescrArtifact).getArtifact().getFile().toURI().toURL();
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
            final ArtifactResolver loaderResolver = MojoUtils.toJsonArtifactResolver(mvn);
            platform = jsonDescrLoader.load(new QuarkusJsonPlatformDescriptorLoaderContext() {
                @Override
                public <T> T parseJson(Function<Path, T> parser) {
                    return parser.apply(jsonFile.toPath());
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

    private String resolveLatestJsonVersion(MavenArtifactResolver mvn, String groupId, String artifactId, String versionRange) {
        if(versionRange == null) {
            versionRange = getProperty(PROP_PLATFORM_JSON_VERSION_RANGE);
            if(versionRange == null) {
                versionRange = "[0,)";
            }
        }
        try {
            return resolveLatestFromVersionRange(mvn, groupId, artifactId, null, "json", versionRange);
        } catch (AppModelResolverException e) {
            throw new IllegalStateException("Failed to resolve the latest JSON platform version of " + groupId + ":" + artifactId + "::json:" + versionRange);
        }
    }

    private static String resolveQuarkusCoreVersion(ArtifactDescriptorResult platformDescr)
            throws AppModelResolverException {
        for(Dependency dep : platformDescr.getManagedDependencies()) {
            final Artifact artifact = dep.getArtifact();
            if(!ToolsConstants.QUARKUS_CORE_ARTIFACT_ID.equals(artifact.getArtifactId())) {
                continue;
            }
            if(!ToolsConstants.QUARKUS_CORE_GROUP_ID.equals(artifact.getGroupId())) {
                continue;
            }
            return artifact.getVersion();
        }
        throw new AppModelResolverException("Failed to locate " + ToolsConstants.QUARKUS_CORE_GROUP_ID + ":" + ToolsConstants.QUARKUS_CORE_ARTIFACT_ID + " among the managed dependencies");
    }

    private String resolveLatestFromVersionRange(MavenArtifactResolver mvn, String groupId, String artifactId, String classifier, String type, final String versionRange)
            throws AppModelResolverException {
        final DefaultArtifact artifact = new DefaultArtifact(groupId, artifactId, classifier, type, versionRange);
        log.debug("Resolving the latest version of " + artifact);
        final VersionRangeResult versionRangeResult = mvn.resolveVersionRange(artifact);
        final Version highestVersion = versionRangeResult.getHighestVersion();
        if(highestVersion == null) {
            throw new IllegalStateException("Failed to resolve the latest version of " + artifact);
        }
        return highestVersion.toString();
    }

    private static String resolveRequired(JsonObject json, String name) {
        final JsonValue value = json.get(name);
        if(value == null) {
            throw new IllegalStateException("Failed to locate '" + name + "' in " + json.toString(WriterConfig.PRETTY_PRINT));
        }
        return value.asString();
    }
}
