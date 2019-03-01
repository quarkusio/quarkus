package io.quarkus.camel.core.deployment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.camel.runtime.CamelRuntime;
import io.quarkus.camel.runtime.CamelRuntimeProducer;
import io.quarkus.camel.runtime.CamelTemplate;
import io.quarkus.camel.runtime.RuntimeRegistry;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;

class CamelInitProcessor {
    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;
    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    AdditionalBeanBuildItem camelRuntimeProducer(BuildProducer<BeanContainerListenerBuildItem> listener, CamelTemplate template,
            CamelRuntimeBuildItem runtimeBuildItem) {
        listener.produce(new BeanContainerListenerBuildItem(template.initRuntimeInjection(runtimeBuildItem.getRuntime())));
        return new AdditionalBeanBuildItem(CamelRuntimeProducer.class);
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(applicationArchiveMarkers = CamelSupport.CAMEL_SERVICE_BASE_PATH)
    CamelRuntimeBuildItem createInitTask(RecorderContext recorderContext, CamelTemplate template) {
        Properties properties = new Properties();
        Config config = ConfigProvider.getConfig();
        for (String i : config.getPropertyNames()) {
            properties.put(i, config.getValue(i, String.class));
        }

        String clazz = properties.getProperty(CamelRuntime.PROP_CAMEL_RUNTIME, CamelRuntime.class.getName());
        RuntimeValue<?> runtime = recorderContext.newInstance(clazz);
        RuntimeRegistry registry = new RuntimeRegistry();
        List<RuntimeValue<?>> builders = getInitRouteBuilderClasses().map(recorderContext::newInstance)
                .collect(Collectors.toList());

        visitServices((name, type) -> registry.bind(name, type, recorderContext.newInstance(type.getName())));

        return new CamelRuntimeBuildItem(template.init(runtime, registry, properties, builders));
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(applicationArchiveMarkers = CamelSupport.CAMEL_SERVICE_BASE_PATH)
    void createRuntimeInitTask(CamelTemplate template, CamelRuntimeBuildItem runtime, ShutdownContextBuildItem shutdown)
            throws Exception {
        template.start(shutdown, runtime.getRuntime());
    }

    protected Stream<String> getInitRouteBuilderClasses() {
        Set<ClassInfo> allKnownImplementors = new HashSet<>();
        allKnownImplementors.addAll(
                combinedIndexBuildItem.getIndex().getAllKnownImplementors(DotName.createSimple(RoutesBuilder.class.getName())));
        allKnownImplementors.addAll(
                combinedIndexBuildItem.getIndex().getAllKnownSubclasses(DotName.createSimple(RouteBuilder.class.getName())));
        allKnownImplementors.addAll(combinedIndexBuildItem.getIndex()
                .getAllKnownSubclasses(DotName.createSimple(AdviceWithRouteBuilder.class.getName())));

        return allKnownImplementors
                .stream()
                .filter(CamelSupport::isConcrete)
                .filter(CamelSupport::isPublic)
                .map(ClassInfo::toString);
    }

    protected void visitServices(BiConsumer<String, Class<?>> consumer) {
        CamelSupport.resources(applicationArchivesBuildItem, CamelSupport.CAMEL_SERVICE_BASE_PATH)
                .forEach(p -> visitService(p, consumer));
    }

    protected void visitService(Path p, BiConsumer<String, Class<?>> consumer) {
        String name = p.getFileName().toString();
        try (InputStream is = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String k = entry.getKey().toString();
                if (k.equals("class")) {
                    String clazz = entry.getValue().toString();
                    Class<?> cl = Class.forName(clazz);

                    consumer.accept(name, cl);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
