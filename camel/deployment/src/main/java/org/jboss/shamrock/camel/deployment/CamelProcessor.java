package org.jboss.shamrock.camel.deployment;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.spi.ExchangeFormatter;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.camel.runtime.CamelDeploymentTemplate;
import org.jboss.shamrock.camel.runtime.CamelRuntime;
import org.jboss.shamrock.camel.runtime.SimpleLazyRegistry;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.buildconfig.BuildConfig.ConfigNode;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.runtime.InjectionInstance;

public class CamelProcessor implements ResourceProcessor {

    @Override
    public int getPriority() {
        return 1;
    }

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        final IndexView index = archiveContext.getCombinedIndex();

        processorContext.addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true");

        // JAXB
        processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        processorContext.addReflectiveClass(true, false, "com.sun.xml.bind.v2.ContextFactory");
        processorContext.addReflectiveClass(true, false, "com.sun.xml.internal.bind.v2.ContextFactory");
        processorContext.addResourceBundle("javax.xml.bind.Messages");

        // Public implementations of Camel interfaces
        Stream.of(Endpoint.class, Consumer.class, Producer.class, TypeConverter.class,
                  ExchangeFormatter.class, GenericFileProcessStrategy.class)
                .map(Class::toString)
                .map(DotName::createSimple)
                .map(index::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .filter(v -> (v.flags() & Modifier.PUBLIC) != 0)
                .forEach(v -> processorContext.addReflectiveClass(true, true, v.name().toString()));
        processorContext.addReflectiveClass(true, false, org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory.class.getName());

        index.getAnnotations(DotName.createSimple(Converter.class.getName()))
                .forEach(v -> {
                            if (v.target().kind() == AnnotationTarget.Kind.CLASS) {
                                processorContext.addReflectiveClass(true, true, v.target().asClass().name().toString());
                            }
                            if (v.target().kind() == AnnotationTarget.Kind.METHOD) {
                                processorContext.addReflectiveMethod(v.target().asMethod());
                            }
                        }
                );

        archiveContext.getAllApplicationArchives().stream()
                .map(arch -> arch.getArchiveRoot().resolve("META-INF/services/org/apache/camel"))
                .filter(Files::isDirectory)
                .flatMap(this::safeWalk)
                .filter(Files::isRegularFile)
                .forEach(p -> handleServiceFile(processorContext, p));

        archiveContext.getAllApplicationArchives().stream()
                .map(arch -> arch.getArchiveRoot().resolve("org/apache/camel"))
                .filter(Files::isDirectory)
                .flatMap(this::safeWalk)
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().equals("jaxb.index"))
                .forEach(p -> handleJaxbFile(processorContext, p));

        SimpleLazyRegistry registry = new SimpleLazyRegistry();
        try (BytecodeRecorder context = processorContext.addStaticInitTask(900)) {
            Stream.of("language", "dataformat", "component")
                    .map(s -> "META-INF/services/org/apache/camel/" + s + "/")
                    .flatMap(r -> archiveContext.getAllApplicationArchives().stream().map(arch -> arch.getArchiveRoot().resolve(r)))
                    .filter(Files::isDirectory)
                    .flatMap(this::safeWalk)
                    .filter(Files::isRegularFile)
                    .forEach(p -> handleComponentDataFormatLanguage(processorContext, context, registry, p));
        }

        Properties properties = new Properties();
        ConfigNode config = archiveContext.getBuildConfig().getApplicationConfig();
        storeProperties(properties, config, "");

        Collection<ClassInfo> runtimes = index.getAllKnownSubclasses(DotName.createSimple(CamelRuntime.class.getName()));
        if (!runtimes.isEmpty()) {
            try (BytecodeRecorder context = processorContext.addDeploymentTask(1000)) {
                CamelDeploymentTemplate template = context.getRecordingProxy(CamelDeploymentTemplate.class);
                template.init();
                for (ClassInfo runtime : runtimes) {
                    processorContext.addReflectiveClass(true, false, runtime.toString());
                    InjectionInstance<? extends CamelRuntime> ii = (InjectionInstance) context.newInstanceFactory(runtime.toString());
                    template.run(ii, registry, properties);
                }
            }
        }
    }

    private void handleComponentDataFormatLanguage(ProcessorContext processorContext, BytecodeRecorder context, SimpleLazyRegistry registry, Path p) {
        String name = p.getFileName().toString();
        try (InputStream is = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String k = entry.getKey().toString();
                if (k.equals("class")) {
                    String clazz = entry.getValue().toString();
                    registry.put(name, context.newInstanceFactory(clazz));
                    processorContext.addReflectiveClass(false, false, clazz);
                } else if (k.endsWith(".class")) {
                    // Used for strategy.factory.class
                    String clazz = entry.getValue().toString();
                    processorContext.addReflectiveClass(false, false, clazz);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleServiceFile(ProcessorContext processorContext, Path p) {
        processorContext.addResource(p.toString().substring(1));
    }

    private void handleJaxbFile(ProcessorContext processorContext, Path p) {
        try {
            String path = p.toAbsolutePath().toString().substring(1);
            processorContext.addResource(path);
            for (String line : Files.readAllLines(p)) {
                if (!line.startsWith("#")) {
                    String clazz = path.replace("/", ".") + "." + line.trim();
                    processorContext.addReflectiveClass(true, false, clazz);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void storeProperties(Properties properties, ConfigNode config, String prefix) {
        String s = config.asString();
        if (s != null && !prefix.isEmpty()) {
            properties.setProperty(prefix, s);
        }
        for (String key : config.getChildKeys()) {
            storeProperties(properties, config.get(key), prefix.isEmpty() ? key : prefix + "." + key);
        }
    }

    private Stream<Path> safeWalk(Path p) {
        try {
            return Files.walk(p);
        } catch (IOException e) {
            throw new IOError(e);
        }
    }

    private static void addAllKnownImplementors(IndexView index, ProcessorContext processorContext, String dotName) {
        index.getAllKnownImplementors(DotName.createSimple(dotName)).forEach(
                v -> {
                    if ((v.flags() & Modifier.PUBLIC) != 0) {
                        processorContext.addReflectiveClass(true, true, v.name().toString());
                    }
                }
        );
    }

}
