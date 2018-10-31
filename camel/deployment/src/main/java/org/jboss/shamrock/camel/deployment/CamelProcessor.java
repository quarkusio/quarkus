package org.jboss.shamrock.camel.deployment;

import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Objects;

import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.camel.runtime.CamelDeploymentTemplate;
import org.jboss.shamrock.camel.runtime.CamelRuntime;
import org.jboss.shamrock.deployment.ArchiveContext;
import org.jboss.shamrock.deployment.ProcessorContext;
import org.jboss.shamrock.deployment.ResourceProcessor;
import org.jboss.shamrock.deployment.codegen.BytecodeRecorder;
import org.jboss.shamrock.runtime.InjectionInstance;

public class CamelProcessor implements ResourceProcessor {

    @Override
    public void process(ArchiveContext archiveContext, ProcessorContext processorContext) throws Exception {
        System.err.println("\nProcess Camel\n");

        final IndexView index = archiveContext.getCombinedIndex();

        processorContext.addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true");

        processorContext.addReflectiveClass(false, false, "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
        processorContext.addReflectiveClass(true, false, "com.sun.xml.bind.v2.ContextFactory");
        processorContext.addReflectiveClass(true, false, "com.sun.xml.internal.bind.v2.ContextFactory");

        processorContext.addResourceBundle("javax.xml.bind.Messages");
        processorContext.addResource("META-INF/services/org/apache/camel/TypeConverter");

        addAllKnownImplementors(index, processorContext, "org.apache.camel.Component");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.Endpoint");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.Consumer");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.Producer");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.TypeConverter");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.spi.DataFormat");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.spi.Language");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.spi.ExchangeFormatter");
        addAllKnownImplementors(index, processorContext, "org.apache.camel.component.file.GenericFileProcessStrategy");
        processorContext.addReflectiveClass(true, false, "org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory");

        index.getAnnotations(DotName.createSimple("org.apache.camel.Converter"))
                .forEach(v -> {
                            if (v.target().kind() == AnnotationTarget.Kind.CLASS) {
                                processorContext.addReflectiveClass(true, true, v.target().asClass().name().toString());
                            }
                            if (v.target().kind() == AnnotationTarget.Kind.METHOD) {
                                processorContext.addReflectiveMethod(v.target().asMethod());
                            }
                        }
                );

        archiveContext.getAllApplicationArchives().forEach(arch -> {
                    Path root = arch.getChildPath("META-INF/services");
                    if (root == null) {
                        return;
                    }

                    FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Objects.requireNonNull(file);
                            Objects.requireNonNull(attrs);
                            System.err.println("Adding resource: " + file.toString().substring(1));
                            processorContext.addResource(file.toString().substring(1));
                            return FileVisitResult.CONTINUE;
                        }
                    };
                    try {
                        Files.walkFileTree(root, visitor);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );
        archiveContext.getAllApplicationArchives().forEach(
                arch -> {
                    Path root = arch.getChildPath("org/apache/camel");
                    if (root == null) {
                        return;
                    }

                    FileVisitor<Path> visitor = new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Objects.requireNonNull(file);
                            Objects.requireNonNull(attrs);

                            String name = file.getFileName().toString();

                            if (Objects.equals("jaxb.index", name)) {
                                String path = file.toAbsolutePath().toString().substring(1);

                                processorContext.addResource(path);

                                Files.readAllLines(file, StandardCharsets.UTF_8).forEach(
                                        line -> {
                                            if (line.startsWith("#")) {
                                                return;
                                            }

                                            processorContext.addReflectiveClass(true, false, path.replace("/", ".") + "." + line.trim());
                                        }
                                );
                            }

                            return FileVisitResult.CONTINUE;
                        }
                    };

                    try {
                        Files.walkFileTree(root, visitor);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        );

        Collection<ClassInfo> runtimes = index.getAllKnownSubclasses(DotName.createSimple(CamelRuntime.class.getName()));
        if (!runtimes.isEmpty()) {
            try (BytecodeRecorder context = processorContext.addDeploymentTask(1000)) {
                CamelDeploymentTemplate template = context.getRecordingProxy(CamelDeploymentTemplate.class);
                template.init();
                for (ClassInfo runtime : runtimes) {

                    processorContext.addReflectiveClass(true, false, runtime.toString());
                    InjectionInstance<? extends CamelRuntime> ii = (InjectionInstance) context.newInstanceFactory(runtime.toString());
                    template.run(ii);
                }
            }
        } else {
            System.err.println("\nNo class implementing CamelRuntime found !\n");
        }
    }

    @Override
    public int getPriority() {
        return 1;
    }

    private static void addAllKnownImplementors(IndexView index, ProcessorContext processorContext, String dotName) {
        index.getAllKnownImplementors(DotName.createSimple(dotName)).forEach(
                v -> {
                    if ((v.flags() & Modifier.PUBLIC) != 0) {
                        System.err.println("Adding reflective: " + v.name().toString());
                        processorContext.addReflectiveClass(true, true, v.name().toString());
                    }
                }
        );
    }
}
