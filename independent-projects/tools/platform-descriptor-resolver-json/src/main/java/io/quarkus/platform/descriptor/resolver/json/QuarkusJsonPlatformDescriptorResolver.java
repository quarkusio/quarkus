package io.quarkus.platform.descriptor.resolver.json;

import static io.quarkus.platform.tools.ToolsUtils.getProperty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Function;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import io.quarkus.bootstrap.model.AppArtifact;
import io.quarkus.bootstrap.resolver.AppModelResolver;
import io.quarkus.bootstrap.resolver.AppModelResolverException;
import io.quarkus.bootstrap.resolver.BootstrapAppModelResolver;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import io.quarkus.bootstrap.resolver.maven.workspace.ModelUtils;
import io.quarkus.maven.utilities.MojoUtils;
import io.quarkus.platform.descriptor.QuarkusPlatformDescriptor;
import io.quarkus.platform.descriptor.loader.json.ArtifactResolver;
import io.quarkus.platform.descriptor.loader.json.ClassPathResourceLoader;
import io.quarkus.platform.descriptor.loader.json.DirectoryResourceLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoader;
import io.quarkus.platform.descriptor.loader.json.QuarkusJsonPlatformDescriptorLoaderContext;
import io.quarkus.platform.descriptor.loader.json.ResourceLoader;
import io.quarkus.platform.descriptor.loader.json.ZipResourceLoader;
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
        this.jsonGroupId = groupId;
        this.jsonArtifactId = artifactId;
        this.jsonVersion = version;
        return resolve();
    }

    public QuarkusPlatformDescriptor resolveLatestFromJson(String groupId, String artifactId, String versionRange) {
        this.jsonGroupId = groupId;
        this.jsonArtifactId = artifactId;
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

        if(log == null) {
            log = new DefaultMessageWriter();
        }

        if(artifactResolver == null) {
            try {
                artifactResolver = new BootstrapAppModelResolver(MavenArtifactResolver.builder().build());
            } catch (AppModelResolverException e) {
                throw new IllegalStateException("Failed to initialize the Maven artifact resolver", e);
            }
        }

        if(jsonDescriptor != null) {
            return loadFromFile(jsonDescriptor);
        }
        return resolveJsonDescriptor(artifactResolver);
    }

    private QuarkusPlatformDescriptor loadFromFile(Path jsonFile) {
        log.debug("Loading Quarkus platform descriptor from %s", jsonFile);
        if(!Files.exists(jsonFile)) {
            throw new IllegalStateException("Failed to locate extensions JSON file at " + jsonFile);
        }

        // Resolve the Quarkus version used by the platform
        final String quarkusCoreVersion;
        try(BufferedReader reader = Files.newBufferedReader(jsonFile)) {
            final JsonObject jsonObject = Json.parse(reader).asObject();
            quarkusCoreVersion = jsonObject.getString("quarkus-core-version", null);
            if(quarkusCoreVersion == null) {
                throw new IllegalStateException("Failed to determine the Quarkus Core version for " + jsonFile);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse extensions JSON file " + jsonFile);
        }
        log.debug("Loaded Quarkus platform is based on Quarkus %s", quarkusCoreVersion);

        try (InputStream is = Files.newInputStream(jsonFile)) {
            return loadPlatformDescriptor(artifactResolver, is, quarkusCoreVersion);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read " + jsonFile, e);
        }
    }

    private QuarkusPlatformDescriptor resolveJsonDescriptor(AppModelResolver artifactResolver) {
        if(bomArtifactId != null) {
            return resolveJsonArtifactFromBom(artifactResolver);
        }
        return resolveJsonArtifactFromArgs(artifactResolver);
    }

    private QuarkusPlatformDescriptor resolveJsonArtifactFromArgs(AppModelResolver artifactResolver) {
        String jsonGroupId = this.jsonGroupId;
        String jsonArtifactId = this.jsonArtifactId;
        String jsonVersion = this.jsonVersion;
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
        return loadFromJsonArtifact(artifactResolver, new AppArtifact(jsonGroupId, jsonArtifactId, null, "json", jsonVersion));
    }

    private QuarkusPlatformDescriptor resolveJsonArtifactFromBom(AppModelResolver artifactResolver) {
        String bomGroupId = this.bomGroupId;
        if(bomGroupId == null) {
            bomGroupId = ToolsConstants.DEFAULT_PLATFORM_BOM_GROUP_ID;
        }
        if(bomArtifactId == null) {
            throw new IllegalStateException("Quarkus Platform BOM artifactId is missing");
        }
        if(bomVersion == null) {
            String bomVersionRange = this.bomVersionRange;
            if(bomVersionRange == null) {
                bomVersionRange = "[0,)";
            }
            final AppArtifact bomArtifact = new AppArtifact(bomGroupId, bomArtifactId, null, "pom", bomVersionRange);
            log.debug("Resolving the latest version of %s", bomArtifact);
            try {
                bomVersion = artifactResolver.getLatestVersionFromRange(bomArtifact, bomVersionRange);
            } catch (AppModelResolverException e) {
                throw new IllegalStateException("Failed to resolve the latest version of " + bomArtifact, e);
            }
            throw new IllegalStateException("Quarkus Platform BOM version is missing");
        }
        log.debug("Resolving Quarkus platform descriptor from %s:%s::pom:%s", bomGroupId, bomArtifactId, bomVersion);

        // first check whether the BOM is available on the classpath
        final InputStream bomIs = Thread.currentThread().getContextClassLoader().getResourceAsStream("quarkus-bom/pom.xml");
        if(bomIs != null) {
            final Model model;
            try {
                model = MojoUtils.readPom(bomIs);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to load POM model from the classpath for quarkus-bom/pom.xml", e);
            } finally {
                try {
                    bomIs.close();
                } catch (IOException e) {
                }
            }
            // Check whether the BOM on the classpath is matching the requested one
            if(bomArtifactId.equals(model.getArtifactId())) {
                final Parent bomParent = model.getParent();
                if(bomVersion.equals(model.getVersion() == null ? bomParent.getVersion() : model.getVersion())
                        && bomGroupId.equals(model.getGroupId() == null ? bomParent.getGroupId() : model.getGroupId())) {
                    log.debug("The requested Quarkus platform BOM version is available on the classpath");
                    // If the BOM matches, there should also be the JSON file
                    final InputStream jsonStream = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream("quarkus-bom-descriptor/extensions.json");
                    if(jsonStream != null) {
                        // The JSON is available, now there also should be quarkus.properties
                        final InputStream quarkusPropsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("quarkus.properties");
                        if(quarkusPropsStream != null) {
                            final Properties props = new Properties();
                            try {
                                props.load(quarkusPropsStream);
                            } catch(IOException e) {
                                throw new IllegalStateException("Failed to load quarkus.properties from the classpath", e);
                            }
                            final String quarkusVersion = props.getProperty("plugin-version");
                            if(quarkusVersion == null) {
                                throw new IllegalStateException("quarkus.properties loaded from the classpath is missing plugin-version property");
                            }
                            return loadPlatformDescriptor(artifactResolver,jsonStream, quarkusVersion);
                        } else {
                            log.debug("Failed to locate quarkus.properties on the classpath");
                        }
                    } else {
                        log.debug("Failed to locate Quarkus JSON platform descriptor on the classpath");
                    }
                }
            }
        }

        // first we are looking for the same GAV as the BOM but with JSON type
        return loadFromJsonArtifact(artifactResolver, new AppArtifact(bomGroupId, bomArtifactId, null, "json", bomVersion));
    }

    private QuarkusPlatformDescriptor loadFromJsonArtifact(AppModelResolver artifactResolver, AppArtifact jsonArtifact) {
        try {
            log.debug("Attempting to resolve Quarkus JSON platform descriptor as %s", jsonArtifact);
            return loadFromFile(artifactResolver.resolve(jsonArtifact));
        } catch (Throwable e) {
            // it didn't work, now we are trying artifactId-descriptor-json
            final AppArtifact fallbackArtifact = new AppArtifact(jsonArtifact.getGroupId(), jsonArtifact.getArtifactId() + "-descriptor-json", null, "json", jsonArtifact.getVersion());
            log.debug("Attempting to resolve Quarkus JSON platform descriptor as %s", fallbackArtifact);
            try {
                return loadFromFile(artifactResolver.resolve(fallbackArtifact));
            } catch (Throwable e1) {
                throw new IllegalStateException("Failed to resolve the JSON descriptor artifact as " + jsonArtifact);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private QuarkusPlatformDescriptor loadPlatformDescriptor(AppModelResolver mvn, final InputStream jsonStream,
            String quarkusCoreVersion) {

        ClassLoader jsonDescrLoaderCl = null;

        // check whether the quarkus-platform-descriptor-json used in the platform is already on the classpath
        final String pomPropsPath = "META-INF/maven/" + ToolsConstants.IO_QUARKUS + "/quarkus-platform-descriptor-json/pom.properties";
        final InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pomPropsPath);
        if(is != null) {
            final Properties props = new Properties();
            try {
                props.load(is);
            } catch(IOException e) {
                throw new IllegalStateException("Failed to load " + pomPropsPath + " from the classpath", e);
            }
            final String version = props.getProperty("version");
            if(quarkusCoreVersion.equals(version)) {
                jsonDescrLoaderCl = Thread.currentThread().getContextClassLoader();
            } else {
                log.debug("Version of the Quarkus JSON platform descriptor loader on the classpath is %s", version);
            }
        }

        // platform resource loader
        ResourceLoader resourceLoader = null;

        boolean externalLoader = false;
        if(jsonDescrLoaderCl == null) {
            final AppArtifact jsonDescrArtifact = new AppArtifact(ToolsConstants.IO_QUARKUS, "quarkus-platform-descriptor-json", null, "jar", quarkusCoreVersion);
            log.debug("Resolving Quarkus JSON platform descriptor loader from %s", jsonDescrArtifact);
            final URL jsonDescrUrl;
            try {
                final Path path = mvn.resolve(jsonDescrArtifact);
                resourceLoader = Files.isDirectory(path) ? new DirectoryResourceLoader(path) : new ZipResourceLoader(path);
                log.debug("Quarkus platform resources will be loaded from %s", path);
                jsonDescrUrl = path.toUri().toURL();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resolve " + jsonDescrArtifact, e);
            }
            jsonDescrLoaderCl = new URLClassLoader(new URL[] {jsonDescrUrl}, Thread.currentThread().getContextClassLoader());
            externalLoader = true;
        }

        try {
            final Iterator<QuarkusJsonPlatformDescriptorLoader> i = ServiceLoader
                    .load(QuarkusJsonPlatformDescriptorLoader.class, jsonDescrLoaderCl).iterator();
            if (!i.hasNext()) {
                throw new IllegalStateException(
                        "Failed to locate an implementation of " + QuarkusJsonPlatformDescriptorLoader.class.getName());
            }
            final QuarkusJsonPlatformDescriptorLoader<?> jsonDescrLoader = i.next();
            if (i.hasNext()) {
                throw new IllegalStateException(
                        "Located more than one implementation of " + QuarkusJsonPlatformDescriptorLoader.class.getName());
            }
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
                    if (!"pom".equals(type)) {
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
                }
            };

            return jsonDescrLoader.load(
                    new QuarkusJsonPlatformDescriptorLoaderContext(
                            loaderResolver,
                            resourceLoader == null ? new ClassPathResourceLoader() : resourceLoader,
                            log) {
                        @Override
                        public <T> T parseJson(Function<InputStream, T> parser) {
                            return parser.apply(jsonStream);
                        }
                    });
        } finally {
            if (externalLoader) {
                try {
                    ((URLClassLoader) jsonDescrLoaderCl).close();
                } catch (IOException e) {
                }
            }
        }
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
