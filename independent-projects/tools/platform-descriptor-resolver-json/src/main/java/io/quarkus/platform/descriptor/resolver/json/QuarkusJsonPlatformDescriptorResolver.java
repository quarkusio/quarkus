package io.quarkus.platform.descriptor.resolver.json;

import static io.quarkus.platform.tools.ToolsUtils.getProperty;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.bootstrap.BootstrapConstants;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.devtools.messagewriter.MessageWriter;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.json.ArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.ClassPathResourceLoader;
import io.quarkus.platform.descriptor.loader.json.DirectoryResourceLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.descriptor.loader.json.ZipResourceLoader;
import io.quarkus.platform.tools.ToolsConstants;
import io.quarkus.platform.tools.ToolsUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

/**
 * Helps resolve the specific or the latest available version of a JSON platform descriptor.
 */
public class QuarkusJsonPlatformDescriptorResolver {

    private static final String BUNDLED_QUARKUS_BOM_PATH = "quarkus-bom/pom.xml";
    private static final String BUNDLED_QUARKUS_PROPERTIES_PATH = "quarkus.properties";
    private static final String BUNDLED_EXTENSIONS_JSON_PATH = "quarkus-bom-descriptor/extensions.json";

    private static final String QUARKUS_PLATFORM_DESCRIPTOR_JSON = "quarkus-platform-descriptor-json";

    private static final String DEFAULT_QUARKUS_PLATFORM_VERSION_RANGE = "[1.0.0.CR2,2)";
    private static final String DEFAULT_NON_QUARKUS_VERSION_RANGE = "[0,)";

    public static final String PROP_PLATFORM_JSON_GROUP_ID = "quarkus.platform.json.groupId";
    public static final String PROP_PLATFORM_JSON_ARTIFACT_ID = "quarkus.platform.json.artifactId";
    public static final String PROP_PLATFORM_JSON_VERSION = "quarkus.platform.json.version";
    public static final String PROP_PLATFORM_JSON_VERSION_RANGE = "quarkus.platform.json.version-range";

    private static final JsonDescriptorResolver jsonDescriptorResolver;
    static {
        jsonDescriptorResolver = new ChainedJsonDescriptorResolver(
                ((bomGroupId, bomArtifactId, bomVersion, jsonResolver, log) -> jsonResolver.resolveArtifact(bomGroupId,
                        bomArtifactId + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX, bomVersion, "json",
                        bomVersion, log)),
                ((bomGroupId, bomArtifactId, bomVersion, jsonResolver, log) -> jsonResolver.resolveArtifact(bomGroupId,
                        bomArtifactId, null, "json", bomVersion, log)),
                ((bomGroupId, bomArtifactId, bomVersion, jsonResolver, log) -> jsonResolver.resolveArtifact(bomGroupId,
                        bomArtifactId + "-descriptor-json", null, "json", bomVersion, log)));
    }

    public static JsonDescriptorResolver jsonDescriptorResolver() {
        return jsonDescriptorResolver;
    }

    public static QuarkusJsonPlatformDescriptorResolver newInstance() {
        return new QuarkusJsonPlatformDescriptorResolver();
    }

    private static String getDefaultVersionRange(String groupId, String artifactId) {
        return ToolsConstants.IO_QUARKUS.equals(groupId)
                && (isDefaultArtifactId(artifactId, "quarkus-bom")
                        || isDefaultArtifactId(artifactId, "quarkus-universe-bom")
                        || "quarkus-bom-descriptor".equals(artifactId))
                                ? DEFAULT_QUARKUS_PLATFORM_VERSION_RANGE
                                : DEFAULT_NON_QUARKUS_VERSION_RANGE;
    }

    private static boolean isDefaultArtifactId(String artifactId, String defaultPrefix) {
        return artifactId.equals(defaultPrefix + BootstrapConstants.PLATFORM_DESCRIPTOR_ARTIFACT_ID_SUFFIX)
                || artifactId.equals(defaultPrefix);
    }

    private String jsonGroupId;
    private String jsonArtifactId;
    private String jsonClassifier;
    private String jsonVersion;
    private String jsonVersionRange;

    private Path jsonDescriptor;

    private String bomGroupId;
    private String bomArtifactId;
    private String bomVersion;
    private String bomVersionRange;

    private AppModelResolver artifactResolver;
    private MessageWriter log;

    public QuarkusJsonPlatformDescriptorResolver() {
    }

    public QuarkusPlatformDescriptor resolveFromJson(String groupId, String artifactId, String version) {
        // for backward compatibility the classifier here is an empty string
        return resolveFromJson(groupId, artifactId, "", version);
    }

    public QuarkusPlatformDescriptor resolveFromJson(String groupId, String artifactId, String classifier, String version) {
        this.jsonGroupId = groupId;
        this.jsonArtifactId = artifactId;
        this.jsonClassifier = classifier;
        this.jsonVersion = version;
        return resolve();
    }

    public QuarkusPlatformDescriptor resolveLatestFromJson(String groupId, String artifactId, String versionRange) {
        // for backward compatibility the classifier here is an empty string
        return resolveLatestFromJson(groupId, artifactId, "", versionRange);
    }

    public QuarkusPlatformDescriptor resolveLatestFromJson(String groupId, String artifactId, String classifier,
            String versionRange) {
        this.jsonGroupId = groupId;
        this.jsonArtifactId = artifactId;
        this.jsonClassifier = classifier;
        this.jsonVersionRange = versionRange;
        return resolve();
    }

    public QuarkusPlatformDescriptor resolveFromJsonArtifactId(String artifactId) {
        this.jsonArtifactId = artifactId;
        return resolve();
    }

    public QuarkusPlatformDescriptor resolveFromJson(Path jsonDescriptor) {
        this.jsonDescriptor = jsonDescriptor;
        return resolve();
    }

    public QuarkusPlatformDescriptor resolveFromBom(String groupId, String artifactId, String version) {
        this.bomGroupId = groupId;
        this.bomArtifactId = artifactId;
        this.bomVersion = version;
        return resolve();
    }

    public QuarkusPlatformDescriptor resolveLatestFromBom(String groupId, String artifactId, String versionRange) {
        this.bomGroupId = groupId;
        this.bomArtifactId = artifactId;
        this.bomVersionRange = versionRange;
        return resolve();
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

        ensureLoggerInitialized();

        AppModelResolver artifactResolver = this.artifactResolver;
        if (artifactResolver == null) {
            try {
                artifactResolver = new BootstrapAppModelResolver(MavenArtifactResolver.builder().build());
            } catch (Exception e) {
                throw new IllegalStateException("Failed to initialize the Maven artifact resolver", e);
            }
        }

        try {
            if (jsonDescriptor != null) {
                return loadFromFile(artifactResolver, jsonDescriptor);
            }
            return resolveJsonDescriptor(artifactResolver);
        } catch (VersionNotAvailableException | PlatformDescriptorLoadingException e) {
            throw new IllegalStateException("Failed to load Quarkus platform descriptor", e);
        }
    }

    public QuarkusPlatformDescriptor resolveBundled() {
        ensureLoggerInitialized();
        final Model bundledBom = loadBundledPom();
        if (bundledBom == null) {
            throw new IllegalStateException("Failed to locate bundled Quarkus platform BOM on the classpath");
        }
        try {
            return loadFromBomCoords(null, getGroupId(bundledBom), getArtifactId(bundledBom), getVersion(bundledBom),
                    bundledBom);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load bundled Quarkus platform", e);
        }
    }

    private void ensureLoggerInitialized() {
        if (log == null) {
            log = MessageWriter.info();
        }
    }

    private QuarkusPlatformDescriptor loadFromFile(AppModelResolver artifactResolver, Path jsonFile)
            throws PlatformDescriptorLoadingException, VersionNotAvailableException {
        log.debug("Loading Quarkus platform descriptor from %s", jsonFile);
        if (!Files.exists(jsonFile)) {
            throw new IllegalArgumentException("Failed to locate extensions JSON file at " + jsonFile);
        }

        // Resolve the Quarkus version used by the platform
        final String quarkusCoreVersion;
        try (BufferedReader reader = Files.newBufferedReader(jsonFile)) {
            JsonNode node = new ObjectMapper().readTree(reader);
            quarkusCoreVersion = node.get("quarkus-core-version").asText(null);
            if (quarkusCoreVersion == null) {
                throw new IllegalStateException("Failed to determine the Quarkus Core version for " + jsonFile);
            }
        } catch (RuntimeException | IOException e) {
            throw new PlatformDescriptorLoadingException("Failed to parse extensions JSON file " + jsonFile, e);
        }
        log.debug("Loaded Quarkus platform is based on Quarkus %s", quarkusCoreVersion);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return loadPlatformDescriptor(toLoaderResolver(artifactResolver), is, quarkusCoreVersion);
        } catch (VersionNotAvailableException e) {
            throw e;
        } catch (Exception e) {
            throw new PlatformDescriptorLoadingException("Failed to load Quarkus platform descriptor from " + jsonFile, e);
        }
    }

    private QuarkusPlatformDescriptor resolveJsonDescriptor(AppModelResolver artifactResolver)
            throws PlatformDescriptorLoadingException {
        if (!ToolsUtils.isNullOrEmpty(bomGroupId) || !ToolsUtils.isNullOrEmpty(bomArtifactId)
                || !ToolsUtils.isNullOrEmpty(bomVersion) || !ToolsUtils.isNullOrEmpty(bomVersionRange)) {
            if (log.isDebugEnabled()) {
                final StringBuilder buf = new StringBuilder();
                buf.append("Resolving Quarkus platform descriptor from the provided BOM coordinates ");
                appendArg(buf, bomGroupId);
                buf.append(":");
                appendArg(buf, bomArtifactId);
                buf.append(":");
                if (!ToolsUtils.isNullOrEmpty(bomVersion)) {
                    appendArg(buf, bomVersion);
                } else if (!ToolsUtils.isNullOrEmpty(bomVersionRange)) {
                    appendArg(buf, bomVersionRange);
                } else {
                    appendArg(buf, bomVersion);
                }
                log.debug(buf.toString());
            }
            return resolveJsonArtifactFromBom(artifactResolver);
        }
        try {
            return resolveJsonArtifactFromArgs(artifactResolver);
        } catch (VersionNotAvailableException e) {
            final QuarkusPlatformDescriptor platform = resolveJsonArtifactFromBom(artifactResolver);
            log.warn(e.getLocalizedMessage() + ", falling back to the bundled platform based on " + platform.getBomGroupId()
                    + ":" + platform.getBomArtifactId() + "::pom:" + platform.getBomVersion() + " and Quarkus version "
                    + platform.getQuarkusVersion());
            return platform;
        }
    }

    private static void appendArg(StringBuilder buf, String arg) {
        buf.append(ToolsUtils.isNullOrEmpty(arg) ? "<not-provided>" : arg);
    }

    private QuarkusPlatformDescriptor resolveJsonArtifactFromArgs(AppModelResolver artifactResolver)
            throws VersionNotAvailableException {
        String jsonGroupId = this.jsonGroupId;
        String jsonArtifactId = this.jsonArtifactId;
        String jsonClassifier = this.jsonClassifier;
        String jsonVersion = this.jsonVersion;
        // If some of the coordinates are missing, we are trying the default ones
        int defaultCoords = 0;
        if (jsonGroupId == null) {
            jsonGroupId = getProperty(PROP_PLATFORM_JSON_GROUP_ID);
            if (jsonGroupId == null) {
                jsonGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
                ++defaultCoords;
            }
        }
        boolean artifactIdProvided = jsonArtifactId != null;
        if (!artifactIdProvided) {
            jsonArtifactId = getProperty(PROP_PLATFORM_JSON_ARTIFACT_ID);
            artifactIdProvided = jsonArtifactId != null;
            if (!artifactIdProvided) {
                jsonArtifactId = ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID;
                ++defaultCoords;
            }
        }
        boolean versionProvided = jsonVersion != null;
        if (!versionProvided) {
            if (jsonVersionRange != null) {
                // if the range was set using the api, it overrides a possibly set version system property
                // depending on how this evolves this may or may not be reasonable
                try {
                    jsonVersion = resolveLatestJsonVersion(artifactResolver, jsonGroupId, jsonArtifactId, jsonVersionRange);
                } catch (VersionNotAvailableException e) {
                    throw new IllegalStateException("Failed to resolve the latest version of " + jsonGroupId + ":"
                            + jsonArtifactId + " from the requested range " + jsonVersionRange, e);
                }
                versionProvided = true;
            } else {
                jsonVersion = getProperty(PROP_PLATFORM_JSON_VERSION);
                versionProvided = jsonVersion != null;
                if (!versionProvided) {
                    jsonVersion = resolveLatestJsonVersion(artifactResolver, jsonGroupId, jsonArtifactId, null);
                    ++defaultCoords;
                }
            }
        }
        if (jsonClassifier == null) {
            jsonClassifier = jsonVersion;
        }
        final AppArtifact jsonArtifact = new AppArtifact(jsonGroupId, jsonArtifactId, jsonClassifier, "json", jsonVersion);
        if (artifactIdProvided) {
            try {
                return loadFromFile(artifactResolver, artifactResolver.resolve(jsonArtifact));
            } catch (PlatformDescriptorLoadingException e) {
                // the artifact was successfully resolved but processing of it has failed
                throw new IllegalStateException("Failed to load Quarkus platform descriptor " + jsonArtifact, e);
            } catch (Exception e) {
                if (!versionProvided && e instanceof VersionNotAvailableException) {
                    throw (VersionNotAvailableException) e;
                }
                throw new IllegalStateException("Failed to resolve Quarkus platform descriptor " + jsonArtifact, e);
            }
        }
        try {
            return loadDescriptorForBom(artifactResolver, jsonArtifact);
        } catch (VersionNotAvailableException e) {
            if (defaultCoords == 3) {
                // complete coords were the default ones, so we can re-throw and try the bundled platform
                throw e;
            }
            throw new IllegalStateException("Failed to resolve the JSON artifact with the requested coordinates", e);
        }
    }

    private QuarkusPlatformDescriptor resolveJsonArtifactFromBom(AppModelResolver artifactResolver)
            throws PlatformDescriptorLoadingException {

        // If some of the coordinates are missing, we are trying the default ones
        boolean tryingDefaultCoords = false;
        String bomGroupId = this.bomGroupId;
        if (bomGroupId == null) {
            bomGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
            tryingDefaultCoords = true;
        }
        String bomArtifactId = this.bomArtifactId;
        if (bomArtifactId == null) {
            bomArtifactId = ToolsConstants.DEFAULT_PLATFORM_BOM_ARTIFACT_ID;
            tryingDefaultCoords = true;
        }
        String bomVersion = this.bomVersion;
        try {
            return loadFromBomCoords(artifactResolver, bomGroupId, bomArtifactId, bomVersion, null);
        } catch (VersionNotAvailableException e) {
            if (!tryingDefaultCoords) {
                throw new IllegalStateException("Failed to resolve the platform BOM using the provided coordinates", e);
            }
            log.debug(
                    "Failed to resolve Quarkus platform BOM using the default coordinates %s:%s:%s, falling back to the bundled Quarkus platform artifacts",
                    bomGroupId, bomArtifactId, bomVersion);
        }

        Model bundledBom = loadBundledPom();
        bomGroupId = this.bomGroupId;
        if (bomGroupId != null) {
            if (!bomGroupId.equals(getGroupId(bundledBom))) {
                throw new IllegalStateException(
                        "Failed to resolve Quarkus platform using the requested BOM groupId " + bomGroupId);
            }
        } else {
            bomGroupId = getGroupId(bundledBom);
        }
        if (bomGroupId == null) {
            failedDetermineDefaultPlatformCoords();
        }

        bomArtifactId = this.bomArtifactId;
        if (bomArtifactId != null) {
            if (!bomArtifactId.equals(getArtifactId(bundledBom))) {
                throw new IllegalStateException(
                        "Failed to resolve Quarkus platform using the requested BOM artifactId " + bomArtifactId);
            }
        } else {
            bomArtifactId = getArtifactId(bundledBom);
        }
        if (bomArtifactId == null) {
            failedDetermineDefaultPlatformCoords();
        }

        bomVersion = this.bomVersion;
        if (bomVersion != null) {
            if (!bomVersion.equals(getVersion(bundledBom))) {
                throw new IllegalStateException(
                        "Failed to resolve Quarkus platform using the requested BOM version " + bomVersion);
            }
        } else if (this.bomVersionRange == null) {
            bomVersion = getVersion(bundledBom);
        }
        try {
            return loadFromBomCoords(artifactResolver, bomGroupId, bomArtifactId, bomVersion, bundledBom);
        } catch (VersionNotAvailableException e) {
            // this should never happen
            throw new IllegalStateException("Failed to load the bundled platform artifacts", e);
        }
    }

    private QuarkusPlatformDescriptor loadFromBomCoords(AppModelResolver artifactResolver, String bomGroupId,
            String bomArtifactId, String bomVersion, Model bundledBom)
            throws PlatformDescriptorLoadingException, VersionNotAvailableException {
        if (bomVersion == null) {
            String bomVersionRange = this.bomVersionRange;
            if (bomVersionRange == null) {
                bomVersionRange = getDefaultVersionRange(bomGroupId, bomArtifactId);
            }
            final AppArtifact bomArtifact = new AppArtifact(bomGroupId, bomArtifactId, null, "pom", bomVersionRange);
            log.debug("Resolving the latest version of %s", bomArtifact);
            try {
                bomVersion = artifactResolver.getLatestVersionFromRange(bomArtifact, bomVersionRange);
            } catch (AppModelResolverException e) {
                throw new VersionNotAvailableException("Failed to resolve the latest version of " + bomArtifact, e);
            }
            if (bomVersion == null) {
                throw new VersionNotAvailableException("Failed to resolve the latest version of " + bomArtifact);
            }
        }
        log.debug("Resolving Quarkus platform BOM %s:%s::pom:%s", bomGroupId, bomArtifactId, bomVersion);

        final Model theBundledBom = loadBundledPomIfNull(bundledBom);
        // Check whether the BOM on the classpath is matching the requested one
        if (theBundledBom != null
                && bomArtifactId.equals(getArtifactId(theBundledBom))
                && bomVersion.equals(getVersion(theBundledBom))
                && bomGroupId.equals(getGroupId(theBundledBom))) {
            log.debug("The requested Quarkus platform BOM version is available on the classpath");
            // If the BOM matches, there should also be the JSON file
            final InputStream jsonStream = getCpResourceAsStream(BUNDLED_EXTENSIONS_JSON_PATH);
            if (jsonStream != null) {
                // The JSON is available, now there also should be quarkus.properties
                final String quarkusVersion = getBundledPlatformQuarkusVersionOrNull();
                if (quarkusVersion != null) {
                    return loadPlatformDescriptor(getBundledResolver(theBundledBom), jsonStream, quarkusVersion);
                } else {
                    log.debug("Failed to locate quarkus.properties on the classpath");
                }
            } else {
                log.debug("Failed to locate Quarkus platform descriptor on the classpath");
            }
        }

        return loadDescriptorForBom(artifactResolver, new AppArtifact(bomGroupId, bomArtifactId, null, "json", bomVersion));
    }

    private void failedDetermineDefaultPlatformCoords() {
        throw new IllegalStateException("Failed to determine the Maven coordinates of the default Quarkus platform");
    }

    private QuarkusPlatformDescriptor loadDescriptorForBom(AppModelResolver artifactResolver, AppArtifact jsonArtifact)
            throws VersionNotAvailableException {
        final Path jsonFile;
        try {
            jsonFile = jsonDescriptorResolver.jsonForBom(jsonArtifact.getGroupId(), jsonArtifact.getArtifactId(),
                    jsonArtifact.getVersion(),
                    (groupId, artifactId, classifier, type, version, log) -> {
                        log.debug("Attempting to resolve Quarkus platform descriptor %s:%s:%s:%s:%s", groupId,
                                artifactId, classifier == null ? "" : classifier, type, version);
                        return artifactResolver
                                .resolve(new AppArtifact(groupId, artifactId, classifier, type, version));
                    },
                    log);
        } catch (Exception e) {
            throw new VersionNotAvailableException("Failed to resolve Quarkus platform descriptor for BOM " + jsonArtifact, e);
        }
        try {
            return loadFromFile(artifactResolver, jsonFile);
        } catch (PlatformDescriptorLoadingException e) {
            throw new IllegalStateException("Failed to load Quarkus platform descriptor " + jsonFile, e);
        }
    }

    @SuppressWarnings("rawtypes")
    private QuarkusPlatformDescriptor loadPlatformDescriptor(ArtifactResolver mvn, final InputStream jsonStream,
            String quarkusCoreVersion) throws PlatformDescriptorLoadingException, VersionNotAvailableException {

        ClassLoader jsonDescrLoaderCl = null;

        // check whether the quarkus-platform-descriptor-json used in the platform is already on the classpath
        final String pomPropsPath = "META-INF/maven/" + ToolsConstants.IO_QUARKUS + "/" + QUARKUS_PLATFORM_DESCRIPTOR_JSON
                + "/pom.properties";
        final InputStream is = getCpResourceAsStream(pomPropsPath);
        if (is != null) {
            final Properties props = new Properties();
            try {
                props.load(is);
            } catch (IOException e) {
                throw new PlatformDescriptorLoadingException("Failed to load " + pomPropsPath + " from the classpath", e);
            }
            final String version = props.getProperty("version");
            if (quarkusCoreVersion.equals(version)) {
                jsonDescrLoaderCl = Thread.currentThread().getContextClassLoader();
            } else {
                log.debug("Version of the Quarkus JSON platform descriptor loader on the classpath is %s", version);
            }
        }

        // platform resource loader
        ResourceLoader resourceLoader = null;

        boolean externalLoader = false;
        if (jsonDescrLoaderCl == null) {
            final AppArtifact jsonDescrArtifact = new AppArtifact(ToolsConstants.IO_QUARKUS, QUARKUS_PLATFORM_DESCRIPTOR_JSON,
                    null, "jar", quarkusCoreVersion);
            log.debug("Resolving Quarkus JSON platform descriptor loader %s", jsonDescrArtifact);
            final URL jsonDescrUrl;
            try {
                final Path path = mvn.process(jsonDescrArtifact.getGroupId(), jsonDescrArtifact.getArtifactId(),
                        jsonDescrArtifact.getClassifier(), jsonDescrArtifact.getType(), jsonDescrArtifact.getVersion(), p -> {
                            return p;
                        });
                resourceLoader = Files.isDirectory(path) ? new DirectoryResourceLoader(path) : new ZipResourceLoader(path);
                log.debug("Quarkus platform resources will be loaded from %s", path);
                jsonDescrUrl = path.toUri().toURL();
            } catch (AppModelResolverException e) {
                throw new VersionNotAvailableException("Failed to resolve " + jsonDescrArtifact, e);
            } catch (Exception e) {
                throw new PlatformDescriptorLoadingException("Failed to resolve " + jsonDescrArtifact, e);
            }
            jsonDescrLoaderCl = new URLClassLoader(new URL[] { jsonDescrUrl }, Thread.currentThread().getContextClassLoader());
            externalLoader = true;
        }

        try {
            final Iterator<QuarkusJsonPlatformDescriptorLoader> i = ServiceLoader
                    .load(QuarkusJsonPlatformDescriptorLoader.class, jsonDescrLoaderCl).iterator();
            if (!i.hasNext()) {
                throw new PlatformDescriptorLoadingException(
                        "Failed to locate an implementation of " + QuarkusJsonPlatformDescriptorLoader.class.getName());
            }
            final QuarkusJsonPlatformDescriptorLoader<?> jsonDescrLoader = i.next();
            if (i.hasNext()) {
                throw new PlatformDescriptorLoadingException(
                        "Located more than one implementation of " + QuarkusJsonPlatformDescriptorLoader.class.getName());
            }

            try {
                return jsonDescrLoader.load(
                        new QuarkusJsonPlatformDescriptorLoaderContext(
                                mvn,
                                resourceLoader == null ? new ClassPathResourceLoader() : resourceLoader,
                                log) {
                            @Override
                            public <T> T parseJson(Function<InputStream, T> parser) {
                                return parser.apply(jsonStream);
                            }
                        });
            } catch (Exception e) {
                throw new PlatformDescriptorLoadingException("Failed to load Quarkus platform descriptor", e);
            }
        } finally {
            if (externalLoader) {
                try {
                    ((URLClassLoader) jsonDescrLoaderCl).close();
                } catch (IOException e) {
                }
            }
        }
    }

    private String resolveLatestJsonVersion(AppModelResolver artifactResolver, String groupId, String artifactId,
            String versionRange) throws VersionNotAvailableException {
        if (versionRange == null) {
            versionRange = getProperty(PROP_PLATFORM_JSON_VERSION_RANGE);
            if (versionRange == null) {
                versionRange = getDefaultVersionRange(groupId, artifactId);
            }
        }
        try {
            return resolveLatestFromVersionRange(artifactResolver, groupId, artifactId, null, "json", versionRange);
        } catch (AppModelResolverException e) {
            throw new VersionNotAvailableException("Failed to resolve the latest JSON platform version of " + groupId + ":"
                    + artifactId + "::json:" + versionRange);
        }
    }

    private String resolveLatestFromVersionRange(AppModelResolver mvn, String groupId, String artifactId, String classifier,
            String type, final String versionRange)
            throws AppModelResolverException, VersionNotAvailableException {
        final AppArtifact appArtifact = new AppArtifact(groupId, artifactId, classifier, type, versionRange);
        log.debug("Resolving the latest version of %s", appArtifact);
        final String latestVersion = mvn.getLatestVersionFromRange(appArtifact, versionRange);
        if (latestVersion == null) {
            throw new VersionNotAvailableException("Failed to resolve the latest version of " + appArtifact);
        }
        return latestVersion;
    }

    private Model loadBundledPomIfNull(Model model) {
        return model == null ? loadBundledPom() : model;
    }

    private Model loadBundledPom() {
        final InputStream bomIs = getCpResourceAsStream(BUNDLED_QUARKUS_BOM_PATH);
        if (bomIs == null) {
            log.debug("Failed to locate quarkus-bom/pom.xml on the classpath");
            return null;
        }
        try {
            return MojoUtils.readPom(bomIs);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load POM model from the classpath for quarkus-bom/pom.xml", e);
        } finally {
            try {
                bomIs.close();
            } catch (IOException e) {
            }
        }
    }

    private static String getGroupId(Model model) {
        if (model == null) {
            return null;
        }
        String groupId = model.getGroupId();
        if (groupId != null) {
            return groupId;
        }
        final Parent parent = model.getParent();
        if (parent != null) {
            groupId = parent.getGroupId();
            if (groupId != null) {
                return groupId;
            }
        }
        throw new IllegalStateException("Failed to determine the groupId for the POM of " + model.getArtifactId());
    }

    private static String getArtifactId(Model model) {
        if (model == null) {
            return null;
        }
        return model.getArtifactId();
    }

    private static String getVersion(Model model) {
        if (model == null) {
            return null;
        }
        String version = model.getVersion();
        if (version != null) {
            return version;
        }
        final Parent parent = model.getParent();
        if (parent != null) {
            version = parent.getVersion();
            if (version != null) {
                return version;
            }
        }
        throw new IllegalStateException("Failed to determine the version for the POM of " + model.getArtifactId());
    }

    private ArtifactResolver toLoaderResolver(AppModelResolver mvn) {
        return new ArtifactResolver() {

            @Override
            public <T> T process(String groupId, String artifactId, String classifier, String type, String version,
                    Function<Path, T> processor) throws AppModelResolverException {
                final AppArtifact artifact = new AppArtifact(groupId, artifactId, classifier, type, version);
                return processor.apply(mvn.resolve(artifact));
            }
        };
    }

    private ArtifactResolver getBundledResolver(final Model model) {

        final Path platformResources;
        try {
            platformResources = MojoUtils.getResourceOrigin(Thread.currentThread().getContextClassLoader(),
                    BUNDLED_QUARKUS_BOM_PATH);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to locate the bundled Quarkus platform resources on the classpath");
        }

        final ArtifactResolver bundledResolver = new ArtifactResolver() {

            @Override
            public <T> T process(String groupId, String artifactId, String classifier, String type, String version,
                    Function<Path, T> processor) {
                if (QUARKUS_PLATFORM_DESCRIPTOR_JSON.equals(artifactId)
                        && ToolsConstants.IO_QUARKUS.equals(groupId)
                        && "jar".equals(type)
                        && StringUtils.isEmpty(classifier)
                        && version.equals(getBundledPlatformQuarkusVersionOrNull())) {
                    return processor.apply(platformResources);
                }
                throw new IllegalArgumentException("Unexpected artifact coordinates " + groupId + ":" + artifactId + ":"
                        + classifier + ":" + type + ":" + version);
            }
        };
        return bundledResolver;
    }

    private static String getBundledPlatformQuarkusVersionOrNull() {
        final InputStream quarkusPropsStream = getCpResourceAsStream(BUNDLED_QUARKUS_PROPERTIES_PATH);
        if (quarkusPropsStream == null) {
            return null;
        }
        final Properties props = new Properties();
        try {
            props.load(quarkusPropsStream);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load quarkus.properties from the classpath", e);
        }
        return ToolsUtils.requireQuarkusCoreVersion(props);
    }

    private static InputStream getCpResourceAsStream(String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }
}
