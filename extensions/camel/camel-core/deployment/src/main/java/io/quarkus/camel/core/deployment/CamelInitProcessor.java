package io.quarkus.camel.core.deployment;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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

import io.quarkus.arc.deployment.RuntimeBeanBuildItem;
import io.quarkus.camel.core.runtime.CamelRuntime;
import io.quarkus.camel.core.runtime.CamelTemplate;
import io.quarkus.camel.core.runtime.RuntimeRegistry;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

class CamelInitProcessor {
    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;
    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;

    @ConfigRoot(phase = ConfigPhase.BUILD_TIME)
    static class CamelConfig {

        /**
         * The class of the CamelRuntime implementation
         */
        @ConfigItem
        Optional<String> runtime;
    }

    CamelConfig config;

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep(applicationArchiveMarkers = { CamelSupport.CAMEL_SERVICE_BASE_PATH, CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY })
    CamelRuntimeBuildItem createInitTask(RecorderContext recorderContext, CamelTemplate template,
            BuildProducer<RuntimeBeanBuildItem> runtimeBeans) {
        Properties properties = new Properties();
        Config configProvider = ConfigProvider.getConfig();
        for (String property : configProvider.getPropertyNames()) {
            if (property.startsWith("camel.")) {
                properties.put(property, configProvider.getValue(property, String.class));
            }
            if (property.startsWith("integration.")) {
                properties.put(property.substring("integration.".length()), configProvider.getValue(property, String.class));
            }
        }

        String clazz = config.runtime.orElse(CamelRuntime.class.getName());
        RuntimeValue<?> runtime = recorderContext.newInstance(clazz);
        RuntimeRegistry registry = new RuntimeRegistry();
        List<RuntimeValue<?>> builders = getInitRouteBuilderClasses().map(recorderContext::newInstance)
                .collect(Collectors.toList());

        visitServices((name, type) -> registry.bind(name, type, recorderContext.newInstance(type.getName())));

        CamelRuntime camelRuntime = template.init(runtime, registry, properties, builders);

        runtimeBeans
                .produce(RuntimeBeanBuildItem.builder(CamelRuntime.class, template.camelRuntimeSupplier(camelRuntime)).build());

        return new CamelRuntimeBuildItem(camelRuntime);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep(applicationArchiveMarkers = { CamelSupport.CAMEL_SERVICE_BASE_PATH, CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY })
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
