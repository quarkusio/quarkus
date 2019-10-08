package io.quarkus.smallrye.openapi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASConfig;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.ApplicationArchive;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.deployment.util.FileUtil;
import io.quarkus.resteasy.deployment.ResteasyJaxrsConfigBuildItem;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentProducer;
import io.quarkus.smallrye.openapi.runtime.OpenApiHandler;
import io.quarkus.vertx.http.deployment.RouteBuildItem;
import io.quarkus.vertx.http.runtime.HandlerType;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;

/**
 * @author Ken Finnigan
 */
public class SmallRyeOpenApiProcessor {

    private static final String META_INF_OPENAPI_YAML = "META-INF/openapi.yaml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YAML = "WEB-INF/classes/META-INF/openapi.yaml";
    private static final String META_INF_OPENAPI_YML = "META-INF/openapi.yml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YML = "WEB-INF/classes/META-INF/openapi.yml";
    private static final String META_INF_OPENAPI_JSON = "META-INF/openapi.json";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_JSON = "WEB-INF/classes/META-INF/openapi.json";

    private static final DotName OPENAPI_SCHEMA = DotName.createSimple(Schema.class.getName());
    private static final DotName OPENAPI_RESPONSE = DotName.createSimple(APIResponse.class.getName());
    private static final DotName OPENAPI_RESPONSES = DotName.createSimple(APIResponses.class.getName());

    private static final String OPENAPI_RESPONSE_CONTENT = "content";
    private static final String OPENAPI_RESPONSE_SCHEMA = "schema";
    private static final String OPENAPI_SCHEMA_NOT = "not";
    private static final String OPENAPI_SCHEMA_ONE_OF = "oneOf";
    private static final String OPENAPI_SCHEMA_ANY_OF = "anyOf";
    private static final String OPENAPI_SCHEMA_ALL_OF = "allOf";
    private static final String OPENAPI_SCHEMA_IMPLEMENTATION = "implementation";
    protected static final String OPEN_API_SCANNING_UTIL = "io.quarkus.smallrye.openapi.deployment.OpenApiScanningUtil";

    SmallRyeOpenApiConfig openapi;

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> configFiles() {
        return Stream.of(META_INF_OPENAPI_YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML,
                META_INF_OPENAPI_YML, WEB_INF_CLASSES_META_INF_OPENAPI_YML,
                META_INF_OPENAPI_JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON).map(HotDeploymentWatchedFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    RouteBuildItem handler() {
        return new RouteBuildItem(openapi.path, new OpenApiHandler(), HandlerType.BLOCKING);
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(OpenApiDocumentProducer.class);
    }

    @BuildStep
    OpenApiFilteredIndexViewBuildItem smallryeOpenApiIndex(CombinedIndexBuildItem combinedIndexBuildItem,
            BeanArchiveIndexBuildItem beanArchiveIndexBuildItem) {
        CompositeIndex compositeIndex = CompositeIndex.create(combinedIndexBuildItem.getIndex(),
                beanArchiveIndexBuildItem.getIndex());
        return new OpenApiFilteredIndexViewBuildItem(
                new FilteredIndexView(
                        compositeIndex,
                        new OpenApiConfigImpl(ConfigProvider.getConfig())));
    }

    @BuildStep
    public void registerOpenApiSchemaClassesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem) {

        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();
        // Generate reflection declaration from MP OpenAPI Schema definition
        // They are needed for serialization.
        Collection<AnnotationInstance> schemaAnnotationInstances = index.getAnnotations(OPENAPI_SCHEMA);
        for (AnnotationInstance schemaAnnotationInstance : schemaAnnotationInstances) {
            AnnotationTarget typeTarget = schemaAnnotationInstance.target();
            if (typeTarget.kind() != AnnotationTarget.Kind.CLASS) {
                continue;
            }
            reflectiveHierarchy
                    .produce(new ReflectiveHierarchyBuildItem(Type.create(typeTarget.asClass().name(), Type.Kind.CLASS)));
        }

        // Generate reflection declaration from MP OpenAPI APIResponse schema definition
        // They are needed for serialization
        Collection<AnnotationInstance> apiResponseAnnotationInstances = index.getAnnotations(OPENAPI_RESPONSE);
        registerReflectionForApiResponseSchemaSerialization(reflectiveClass, reflectiveHierarchy,
                apiResponseAnnotationInstances);

        // Generate reflection declaration from MP OpenAPI APIResponses schema definition
        // They are needed for serialization
        Collection<AnnotationInstance> apiResponsesAnnotationInstances = index.getAnnotations(OPENAPI_RESPONSES);
        for (AnnotationInstance apiResponsesAnnotationInstance : apiResponsesAnnotationInstances) {
            AnnotationValue apiResponsesAnnotationValue = apiResponsesAnnotationInstance.value();
            if (apiResponsesAnnotationValue == null) {
                continue;
            }
            registerReflectionForApiResponseSchemaSerialization(reflectiveClass, reflectiveHierarchy,
                    Arrays.asList(apiResponsesAnnotationValue.asNestedArray()));
        }
    }

    private void registerReflectionForApiResponseSchemaSerialization(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy,
            Collection<AnnotationInstance> apiResponseAnnotationInstances) {
        for (AnnotationInstance apiResponseAnnotationInstance : apiResponseAnnotationInstances) {
            AnnotationValue contentAnnotationValue = apiResponseAnnotationInstance.value(OPENAPI_RESPONSE_CONTENT);
            if (contentAnnotationValue == null) {
                continue;
            }

            AnnotationInstance[] contents = contentAnnotationValue.asNestedArray();
            for (AnnotationInstance content : contents) {
                AnnotationValue annotationValue = content.value(OPENAPI_RESPONSE_SCHEMA);
                if (annotationValue == null) {
                    continue;
                }
                AnnotationInstance schema = annotationValue.asNested();
                AnnotationValue schemaImplementationClass = schema.value(OPENAPI_SCHEMA_IMPLEMENTATION);
                if (schemaImplementationClass != null) {
                    reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(schemaImplementationClass.asClass()));
                }

                AnnotationValue schemaNotClass = schema.value(OPENAPI_SCHEMA_NOT);
                if (schemaNotClass != null) {
                    reflectiveClass.produce(new ReflectiveClassBuildItem(true, true, schemaNotClass.asString()));
                }

                AnnotationValue schemaOneOfClasses = schema.value(OPENAPI_SCHEMA_ONE_OF);
                if (schemaOneOfClasses != null) {
                    for (Type schemaOneOfClass : schemaOneOfClasses.asClassArray()) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(schemaOneOfClass));
                    }
                }

                AnnotationValue schemaAnyOfClasses = schema.value(OPENAPI_SCHEMA_ANY_OF);
                if (schemaAnyOfClasses != null) {
                    for (Type schemaAnyOfClass : schemaAnyOfClasses.asClassArray()) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(schemaAnyOfClass));
                    }
                }

                AnnotationValue schemaAllOfClasses = schema.value(OPENAPI_SCHEMA_ALL_OF);
                if (schemaAllOfClasses != null) {
                    for (Type schemaAllOfClass : schemaAllOfClasses.asClassArray()) {
                        reflectiveHierarchy.produce(new ReflectiveHierarchyBuildItem(schemaAllOfClass));
                    }
                }
            }
        }
    }

    @BuildStep
    public void build(ApplicationArchivesBuildItem archivesBuildItem,
            BuildProducer<FeatureBuildItem> feature,
            Optional<ResteasyJaxrsConfigBuildItem> resteasyJaxrsConfig,
            BuildProducer<GeneratedResourceBuildItem> resourceBuildItemBuildProducer,
            BuildProducer<SubstrateResourceBuildItem> substrateResources,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem) throws Exception {

        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_OPENAPI));

        //TODO: MASSIVE HACK ALERT
        //see https://github.com/quarkusio/quarkus/issues/4329#issuecomment-539454820
        //As soon as this is fixed in Smallrye this commit should be reverted
        //if this code is still here once smallrye openapi 1.1.10 is in there has been a mistake

        HackClassLoader cl = new HackClassLoader(archivesBuildItem);
        Class<?> util = cl.loadClass(OPEN_API_SCANNING_UTIL);
        Method generateStaticModel = util.getDeclaredMethod("generateStaticModel", ApplicationArchivesBuildItem.class);
        Method generateAnnotationModel = util.getDeclaredMethod("generateAnnotationModel", IndexView.class,
                ResteasyJaxrsConfigBuildItem.class);
        Method loadDocument = util.getDeclaredMethod("loadDocument", OpenAPI.class, OpenAPI.class);

        OpenAPI staticModel = (OpenAPI) generateStaticModel.invoke(null, archivesBuildItem);

        OpenAPI annotationModel;
        Config config = ConfigProvider.getConfig();
        boolean scanDisable = config.getOptionalValue(OASConfig.SCAN_DISABLE, Boolean.class).orElse(false);
        if (resteasyJaxrsConfig.isPresent() && !scanDisable) {
            annotationModel = (OpenAPI) generateAnnotationModel.invoke(null, index, resteasyJaxrsConfig.get());
        } else {
            annotationModel = null;
        }
        Map<String, byte[]> finalDocument = (Map<String, byte[]>) loadDocument.invoke(null, staticModel, annotationModel);
        for (Map.Entry<String, byte[]> e : finalDocument.entrySet()) {
            resourceBuildItemBuildProducer.produce(new GeneratedResourceBuildItem(e.getKey(), e.getValue()));
            substrateResources.produce(new SubstrateResourceBuildItem(e.getKey()));
        }
    }

    public

    @BuildStep LogCleanupFilterBuildItem logCleanup() {
        return new LogCleanupFilterBuildItem("io.smallrye.openapi.api.OpenApiDocument",
                "OpenAPI document initialized:");
    }

    static class HackClassLoader extends ClassLoader {

        private final ApplicationArchivesBuildItem archivesBuildItem;

        HackClassLoader(ApplicationArchivesBuildItem archivesBuildItem) {
            super(HackClassLoader.class.getClassLoader());
            this.archivesBuildItem = archivesBuildItem;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            return loadClass(name, false);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c != null) {
                return c;
            }
            if (name.startsWith("java.")) {
                return super.loadClass(name, resolve);
            }
            ApplicationArchive applicationArchive = archivesBuildItem.containingArchive(name);
            if (applicationArchive != null) {

                try {
                    try (InputStream res = Files
                            .newInputStream(applicationArchive.getChildPath(name.replace(".", "/") + ".class"))) {
                        byte[] data = FileUtil.readFileContents(res);
                        return defineClass(name, data, 0, data.length);
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException("IO Exception", e);
                }
            }
            if (name.startsWith(OPEN_API_SCANNING_UTIL) || name.startsWith("io.smallrye.openapi")
                    || name.equals("io.quarkus.smallrye.openapi.deployment.RESTEasyExtension")) {
                try {
                    try (InputStream res = Thread.currentThread().getContextClassLoader()
                            .getResourceAsStream(name.replace(".", "/") + ".class")) {
                        if (res != null) {
                            byte[] data = FileUtil.readFileContents(res);
                            return defineClass(name, data, 0, data.length);
                        }
                    }
                } catch (IOException e) {
                    throw new ClassNotFoundException("IO Exception", e);
                }
            }
            return super.loadClass(name, resolve);
        }

    }
}
