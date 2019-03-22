package io.quarkus.camel.core.deployment;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.camel.Consumer;
import org.apache.camel.Converter;
import org.apache.camel.Endpoint;
import org.apache.camel.Producer;
import org.apache.camel.TypeConverter;
import org.apache.camel.component.file.GenericFile;
import org.apache.camel.component.file.GenericFileProcessStrategy;
import org.apache.camel.component.file.strategy.GenericFileProcessStrategyFactory;
import org.apache.camel.converter.jaxp.XmlConverter;
import org.apache.camel.spi.ExchangeFormatter;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;

import io.quarkus.camel.core.runtime.CamelConfig.BuildTime;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentConfigFileBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveMethodBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateConfigBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBundleBuildItem;
import io.quarkus.jaxb.deployment.JaxbEnabledBuildItem;
import io.quarkus.jaxb.deployment.JaxbFileRootBuildItem;

class CamelProcessor {

    private static final List<Class<?>> CAMEL_REFLECTIVE_CLASSES = Arrays.asList(
            Endpoint.class,
            Consumer.class,
            Producer.class,
            TypeConverter.class,
            ExchangeFormatter.class,
            GenericFileProcessStrategy.class);

    private static final List<Class<? extends Annotation>> CAMEL_REFLECTIVE_ANNOTATIONS = Arrays.asList(
            Converter.class);

    @Inject
    BuildProducer<ReflectiveClassBuildItem> reflectiveClass;
    @Inject
    BuildProducer<ReflectiveMethodBuildItem> reflectiveMethod;
    @Inject
    BuildProducer<SubstrateResourceBuildItem> resource;
    @Inject
    BuildProducer<SubstrateResourceBundleBuildItem> resourceBundle;
    @Inject
    ApplicationArchivesBuildItem applicationArchivesBuildItem;
    @Inject
    CombinedIndexBuildItem combinedIndexBuildItem;
    @Inject
    BuildTime buildTimeConfig;

    @BuildStep
    JaxbFileRootBuildItem fileRoot() {
        return new JaxbFileRootBuildItem(CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY);
    }

    @BuildStep
    JaxbEnabledBuildItem enableJaxb() {
        return buildTimeConfig.disableJaxb ? null : new JaxbEnabledBuildItem();
    }

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FeatureBuildItem.CAMEL_CORE);
    }

    @BuildStep
    SubstrateConfigBuildItem processSystemProperties() {
        return SubstrateConfigBuildItem.builder()
                .addNativeImageSystemProperty("CamelSimpleLRUCacheFactory", "true")
                .build();
    }

    @BuildStep
    List<HotDeploymentConfigFileBuildItem> configFile() {
        return buildTimeConfig.routesUris.stream()
                .map(String::trim)
                .filter(s -> s.startsWith("file:"))
                .map(s -> s.substring("file:".length()))
                .map(HotDeploymentConfigFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep(applicationArchiveMarkers = { CamelSupport.CAMEL_SERVICE_BASE_PATH, CamelSupport.CAMEL_ROOT_PACKAGE_DIRECTORY })
    void process() {
        IndexView view = combinedIndexBuildItem.getIndex();

        CAMEL_REFLECTIVE_CLASSES.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .map(view::getAllKnownImplementors)
                .flatMap(Collection::stream)
                .filter(CamelSupport::isPublic)
                .forEach(v -> addReflectiveClass(true, v.name().toString()));

        CAMEL_REFLECTIVE_ANNOTATIONS.stream()
                .map(Class::getName)
                .map(DotName::createSimple)
                .map(view::getAnnotations)
                .flatMap(Collection::stream)
                .forEach(v -> {
                    if (v.target().kind() == AnnotationTarget.Kind.CLASS) {
                        addReflectiveClass(true, v.target().asClass().name().toString());
                    }
                    if (v.target().kind() == AnnotationTarget.Kind.METHOD) {
                        addReflectiveMethod(v.target().asMethod());
                    }
                });

        addReflectiveClass(false, GenericFile.class.getName());
        addReflectiveClass(true, GenericFileProcessStrategyFactory.class.getName());
        addReflectiveClass(true, XmlConverter.class.getName());

        addCamelServices();
    }

    // Camel services files
    protected void addCamelServices() {
        CamelSupport.resources(applicationArchivesBuildItem, CamelSupport.CAMEL_SERVICE_BASE_PATH)
                .forEach(this::addCamelService);
    }

    protected void addCamelService(Path p) {
        try (InputStream is = Files.newInputStream(p)) {
            Properties props = new Properties();
            props.load(is);
            for (Map.Entry<Object, Object> entry : props.entrySet()) {
                String k = entry.getKey().toString();
                if (k.equals("class")) {
                    addReflectiveClass(true, entry.getValue().toString());
                } else if (k.endsWith(".class")) {
                    addReflectiveClass(true, entry.getValue().toString());
                    addResource(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void addResource(Path p) {
        addResource(p.toString().substring(1));
    }

    protected void addResource(String r) {
        resource.produce(new SubstrateResourceBuildItem(r));
    }

    protected void addReflectiveClass(boolean methods, String... className) {
        reflectiveClass.produce(new ReflectiveClassBuildItem(methods, false, className));
    }

    protected void addReflectiveMethod(MethodInfo mi) {
        reflectiveMethod.produce(new ReflectiveMethodBuildItem(mi));
    }

    protected void addResourceBundle(String bundle) {
        resourceBundle.produce(new SubstrateResourceBundleBuildItem(bundle));
    }
}
