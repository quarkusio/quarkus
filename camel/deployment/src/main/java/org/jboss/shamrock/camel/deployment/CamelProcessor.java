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

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.Language;
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

        Collection<ClassInfo> runtimes = index.getAllKnownSubclasses(DotName.createSimple(CamelRuntime.class.getName()));
        if (runtimes.isEmpty()) {
            return;
        }

        try (Processor processor = new Processor(archiveContext, processorContext, index, runtimes)) {
            processor.process();
        }
    }

    static class Processor implements AutoCloseable {

        final ArchiveContext archiveContext;
        final ProcessorContext processorContext;
        final IndexView index;
        final Collection<ClassInfo> runtimes;
        final BytecodeRecorder recorder;
        final CamelDeploymentTemplate camelTemplate;
        final SimpleLazyRegistry registry;

        Processor(ArchiveContext archiveContext, ProcessorContext processorContext, IndexView index, Collection<ClassInfo> runtimes) {
            this.archiveContext = archiveContext;
            this.processorContext = processorContext;
            this.index = index;
            this.runtimes = runtimes;
            this.recorder = processorContext.addDeploymentTask(1000);
            this.camelTemplate = recorder.getRecordingProxy(CamelDeploymentTemplate.class);
            this.registry = new SimpleLazyRegistry();
        }

        @Override
        public void close() throws Exception {
            recorder.close();
        }

        private void process() throws Exception {
            processorContext.addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true");

            // JAXB
            processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            processorContext.addReflectiveClass(true, false, "com.sun.xml.bind.v2.ContextFactory");
            processorContext.addReflectiveClass(true, false, "com.sun.xml.internal.bind.v2.ContextFactory");
            processorContext.addResourceBundle("javax.xml.bind.Messages");
            iterateResources("org/apache/camel")
                    .filter(p -> p.getFileName().toString().equals("jaxb.index"))
                    .forEach(this::handleJaxbFile);

            // Camel services files
            iterateResources("META-INF/services/org/apache/camel")
                    .forEach(this::addResource);

            // Components, dataformats and languages
            iterateResources("META-INF/services/org/apache/camel/language/")
                    .forEach(p -> handleComponentDataFormatLanguage(Language.class, p));
            iterateResources("META-INF/services/org/apache/camel/dataformat/")
                    .forEach(p -> handleComponentDataFormatLanguage(DataFormat.class, p));
            iterateResources("META-INF/services/org/apache/camel/component/")
                    .forEach(p -> handleComponentDataFormatLanguage(Component.class, p));

            // Public implementations of Camel interfaces
            Stream.of(Endpoint.class, Consumer.class, Producer.class, TypeConverter.class,
                            ExchangeFormatter.class, GenericFileProcessStrategy.class)
                    .map(Class::getName)
                    .map(DotName::createSimple)
                    .map(index::getAllKnownImplementors)
                    .flatMap(Collection::stream)
                    .filter(this::isPublic)
                    .forEach(v -> processorContext.addReflectiveClass(true, true, v.name().toString()));

            // GenericFileProcessStrategyFactory is used through reflection
            processorContext.addReflectiveClass(true, false, GenericFileProcessStrategyFactory.class.getName());

            // Converters
            index.getAnnotations(DotName.createSimple(Converter.class.getName()))
                    .forEach(v -> {
                                if (v.target().kind() == AnnotationTarget.Kind.CLASS) {
                                    processorContext.addReflectiveClass(true, false, v.target().asClass().name().toString());
                                }
                                if (v.target().kind() == AnnotationTarget.Kind.METHOD) {
                                    processorContext.addReflectiveMethod(v.target().asMethod());
                                }
                            }
                    );

            Properties properties = new Properties();
            ConfigNode config = archiveContext.getBuildConfig().getApplicationConfig();
            storeProperties(properties, config, "");

            for (ClassInfo runtime : runtimes) {
                String clazz = runtime.toString();
                processorContext.addReflectiveClass(true, false, clazz);
                camelTemplate.run(createRuntimeInstanceFactory(clazz), registry, properties);
            }
        }

        @SuppressWarnings("unchecked")
        private InjectionInstance<? extends CamelRuntime> createRuntimeInstanceFactory(String clazz) {
            return (InjectionInstance) recorder.newInstanceFactory(clazz);
        }

        private boolean isPublic(ClassInfo ci) {
            return (ci.flags() & Modifier.PUBLIC) != 0;
        }

        private Stream<Path> iterateResources(String path) {
            return archiveContext.getAllApplicationArchives().stream()
                    .map(arch -> arch.getArchiveRoot().resolve(path))
                    .filter(Files::isDirectory)
                    .flatMap(this::safeWalk)
                    .filter(Files::isRegularFile);
        }

        private void handleComponentDataFormatLanguage(Class<?> type, Path p) {
            String name = p.getFileName().toString();
            try (InputStream is = Files.newInputStream(p)) {
                Properties props = new Properties();
                props.load(is);
                for (Map.Entry<Object, Object> entry : props.entrySet()) {
                    String k = entry.getKey().toString();
                    if (k.equals("class")) {
                        String clazz = entry.getValue().toString();
                        registry.bind(name, type, recorder.newInstanceFactory(clazz));
                        processorContext.addReflectiveClass(true, false, clazz);
                    } else if (k.endsWith(".class")) {
                        // Used for strategy.factory.class
                        String clazz = entry.getValue().toString();
                        processorContext.addReflectiveClass(true, false, clazz);
                        addResource(p);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        private void addResource(Path p) {
            processorContext.addResource(p.toString().substring(1));
        }

        private void handleJaxbFile(Path p) {
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
    }

}
