package io.quarkus.freemarker;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FilenameUtils;
import org.jboss.logging.Logger;

import freemarker.ext.jython.JythonModel;
import freemarker.ext.jython.JythonWrapper;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.nativeimage.NativeImageResourceBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.RuntimeInitializedClassBuildItem;
import io.quarkus.freemarker.runtime.FreemarkerBuildConfig;
import io.quarkus.freemarker.runtime.FreemarkerConfigurationProducer;
import io.quarkus.freemarker.runtime.FreemarkerTemplateProducer;
import io.quarkus.freemarker.runtime.TemplatePath;

public class FreemarkerProcessor {

    private static final Logger LOGGER = Logger.getLogger(FreemarkerProcessor.class);

    private static final String FEATURE = "freemarker";

    private static final String CLASSPATH_PROTOCOL = "classpath";

    private static final String JAR_PROTOCOL = "jar";

    private static final String FILE_PROTOCOL = "file";

    private static final String ADD_MSG = "Adding application freemarker templates in path ''{0}'' using protocol ''{1}''";

    private static final String UNSUPPORTED_MSG = "Unsupported URL protocol ''{0}'' for path ''{1}''. Freemarker files will not be discovered.";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void runtimeInit(BuildProducer<RuntimeInitializedClassBuildItem> runtimeInitialized) {

        Stream.of(JythonWrapper.class, JythonModel.class)
                .map(Class::getCanonicalName)
                .map(RuntimeInitializedClassBuildItem::new)
                .forEach(runtimeInitialized::produce);
    }

    @BuildStep
    void discoverTemplates(BuildProducer<NativeImageResourceBuildItem> resourceProducer, FreemarkerBuildConfig config)
            throws IOException, URISyntaxException {

        final List<String> nativeResources = discoverTemplates(config.resourcePaths);
        resourceProducer.produce(new NativeImageResourceBuildItem(nativeResources.toArray(new String[0])));
    }

    @BuildStep
    AdditionalBeanBuildItem additionalBeans() {
        return AdditionalBeanBuildItem.builder()
                .setUnremovable()
                .addBeanClasses(FreemarkerConfigurationProducer.class, TemplatePath.class, FreemarkerTemplateProducer.class,
                        FreemarkerTemplateProducer.class)
                .build();
    }

    @BuildStep
    public void reflection(BuildProducer<ReflectiveClassBuildItem> additionalBeanBuildItemProducer,
            FreemarkerBuildConfig config) {

        LOGGER.info("Adding directives " + config.directive.values());
        config.directive.values().stream()
                .map(classname -> new ReflectiveClassBuildItem(false, false, classname))
                .forEach(additionalBeanBuildItemProducer::produce);
    }

    private List<String> discoverTemplates(List<String> locations) throws IOException, URISyntaxException {

        List<String> resources = new ArrayList<>();

        for (String location : locations) {

            // Strip any 'classpath:' protocol prefixes because they are assumed
            // but not recognized by ClassLoader.getResources()
            if (location.startsWith(CLASSPATH_PROTOCOL + ':')) {
                location = location.substring(CLASSPATH_PROTOCOL.length() + 1);
            }

            Enumeration<URL> templates = Thread.currentThread().getContextClassLoader().getResources(location);

            while (templates.hasMoreElements()) {
                URL path = templates.nextElement();
                LOGGER.infov(ADD_MSG, path.getPath(), path.getProtocol());
                final Set<String> freemarkerTemplates;
                if (JAR_PROTOCOL.equals(path.getProtocol())) {
                    try (final FileSystem fileSystem = initFileSystem(path.toURI())) {
                        freemarkerTemplates = getTemplatesFromPath(location, path);
                    }
                } else if (FILE_PROTOCOL.equals(path.getProtocol())) {
                    freemarkerTemplates = getTemplatesFromPath(location, path);
                } else {
                    LOGGER.warnv(UNSUPPORTED_MSG, path.getProtocol(), path.getPath());
                    freemarkerTemplates = null;
                }

                if (freemarkerTemplates != null) {
                    resources.addAll(freemarkerTemplates);
                }
            }
        }
        return resources;
    }

    private Set<String> getTemplatesFromPath(final String location, final URL pathURL)
            throws IOException, URISyntaxException {

        try (final Stream<Path> pathStream = Files.walk(Paths.get(pathURL.toURI()))) {
            return pathStream
                    .filter(Files::isRegularFile)
                    .map(path -> getSubpath(location, path))
                    .peek(subpath -> LOGGER.info("Discovered: " + subpath))
                    .collect(Collectors.toSet());
        }
    }

    private String getSubpath(String location, Path path) {
        String file = FilenameUtils.separatorsToUnix(path.toString());
        int indexOf = file.lastIndexOf("/" + location + "/");
        if (indexOf == -1) {
            indexOf = file.lastIndexOf("/" + location);
        }
        String substring = file.substring(indexOf + 1);
        return FilenameUtils.separatorsToUnix(substring);
    }

    private FileSystem initFileSystem(final URI uri) throws IOException {
        final Map<String, String> env = new HashMap<>();
        env.put("create", "true");
        return FileSystems.newFileSystem(uri, env);
    }

}
