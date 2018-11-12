package org.jboss.shamrock.camel.deployment;

import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyAttribute;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttachmentRef;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementRefs;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlInlineBinaryData;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlMimeType;
import javax.xml.bind.annotation.XmlMixed;
import javax.xml.bind.annotation.XmlNs;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSchema;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlSchemaTypes;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory;
import org.apache.camel.impl.converter.DoubleMap;
import org.apache.camel.spi.DataFormat;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.Language;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.camel.runtime.CamelRuntime;
import org.jboss.shamrock.camel.runtime.CamelTemplate;
import org.jboss.shamrock.camel.runtime.RuntimeRegistry;
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
        new Processor(archiveContext, processorContext).process();
    }

    static class Processor {

        final ArchiveContext archiveContext;
        final ProcessorContext processorContext;
        final IndexView index;
        final RuntimeRegistry registry;
        final DoubleMap<String, Class<?>, String> components = new DoubleMap<>(128);

        Processor(ArchiveContext archiveContext, ProcessorContext processorContext) {
            this.archiveContext = archiveContext;
            this.processorContext = processorContext;
            this.index = archiveContext.getCombinedIndex();
            this.registry = new RuntimeRegistry();
        }

        private void process() throws Exception {
            processorContext.addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true");

            // JAXB
            processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
            processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
            processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl");            
            processorContext.addReflectiveClass(true, false, "com.sun.xml.bind.v2.ContextFactory");
            processorContext.addReflectiveClass(true, false, "com.sun.xml.internal.bind.v2.ContextFactory");
            Stream.of(XmlAccessOrder.class, XmlAccessorType.class, XmlAnyAttribute.class, XmlAnyElement.class, XmlAttachmentRef.class, XmlAttribute.class,
                    XmlElement.class, XmlElementDecl.class, XmlElementRef.class, XmlElementRefs.class, XmlElements.class, XmlElementWrapper.class,
                    XmlEnum.class, XmlEnumValue.class, XmlID.class, XmlIDREF.class, XmlInlineBinaryData.class, XmlList.class, XmlMimeType.class,
                    XmlMixed.class, XmlNs.class, XmlRegistry.class, XmlRootElement.class, XmlSchema.class, XmlSchemaType.class, XmlSchemaTypes.class,
                    XmlSeeAlso.class, XmlTransient.class, XmlType.class, XmlValue.class,
                    XmlJavaTypeAdapter.class, XmlJavaTypeAdapters.class)
                    .map(Class::getName)
                    .forEach(clazz -> processorContext.addReflectiveClass(true, false, clazz));
            processorContext.addResourceBundle("javax.xml.bind.Messages");
            processorContext.addResourceBundle("com.sun.org.apache.xml.internal.serializer.utils.SerializerMessages");
            processorContext.addResourceBundle("com.sun.org.apache.xml.internal.res.XMLErrorResources");
            Stream.of("html", "text", "xml", "unknown")
                    .map(s -> "com/sun/org/apache/xml/internal/serializer/output_" + s + ".properties")
                    .forEach(processorContext::addResource);
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
            processorContext.addReflectiveClass(false, false, GenericFile.class.getName());

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

            List<String> builders = index.getAllKnownImplementors(DotName.createSimple(RoutesBuilder.class.getName()))
                    .stream()
                    .filter(this::isConcrete)
                    .filter(this::isPublic)
                    .map(ClassInfo::toString)
                    .collect(Collectors.toList());

            try (BytecodeRecorder recorder = processorContext.addStaticInitTask(1000)) {

                String clazz = properties.getProperty(CamelRuntime.PROP_CAMEL_RUNTIME, CamelRuntime.class.getName());
                InjectionInstance<CamelRuntime> iruntime = (InjectionInstance) recorder.newInstanceFactory(clazz);
                RuntimeRegistry registry = new RuntimeRegistry();

                components.forEach((n, c, o) -> registry.bind(n, c, recorder.newInstanceFactory(o)));
                List<InjectionInstance<RoutesBuilder>> ibuilders = (List) builders.stream().map(recorder::newInstanceFactory).collect(Collectors.toList());

                CamelTemplate template = recorder.getRecordingProxy(CamelTemplate.class);
                template.init(iruntime, registry, properties, ibuilders);
            }
            try (BytecodeRecorder recorder = processorContext.addDeploymentTask(1001)) {
                CamelTemplate template = recorder.getRecordingProxy(CamelTemplate.class);
                template.start(null);
            }
        }

        private boolean isConcrete(ClassInfo ci) {
            return (ci.flags() & Modifier.ABSTRACT) == 0;
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
                        components.put(name, type, clazz);
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
                String pkg = p.toAbsolutePath().getParent().toString().substring(1).replace("/", ".") + ".";
                processorContext.addResource(path);
                for (String line : Files.readAllLines(p)) {
                    if (!line.startsWith("#")) {
                        String clazz = pkg + line.trim();
                        Class cl = Class.forName(clazz);
                        while (cl != Object.class) {
                            processorContext.addReflectiveClass(true, true, cl.getName());
                            cl = cl.getSuperclass();
                        }
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
