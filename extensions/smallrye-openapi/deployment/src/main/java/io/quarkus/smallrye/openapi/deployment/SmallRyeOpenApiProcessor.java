package io.quarkus.smallrye.openapi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
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
import org.jboss.jandex.Indexer;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanArchiveIndexBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.resteasy.deployment.ResteasyJaxrsConfigBuildItem;
import io.quarkus.smallrye.openapi.common.deployment.SmallRyeOpenApiConfig;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentProducer;
import io.quarkus.smallrye.openapi.runtime.OpenApiServlet;
import io.quarkus.undertow.deployment.GeneratedWebResourceBuildItem;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.FilteredIndexView;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;

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

    SmallRyeOpenApiConfig openapi;

    @BuildStep
    List<HotDeploymentWatchedFileBuildItem> configFiles() {
        return Stream.of(META_INF_OPENAPI_YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML,
                META_INF_OPENAPI_YML, WEB_INF_CLASSES_META_INF_OPENAPI_YML,
                META_INF_OPENAPI_JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON).map(HotDeploymentWatchedFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    ServletBuildItem servlet() {
        return ServletBuildItem.builder("openapi", OpenApiServlet.class.getName()).addMapping(openapi.path).build();
    }

    @BuildStep
    AdditionalBeanBuildItem beans() {
        return new AdditionalBeanBuildItem(OpenApiServlet.class, OpenApiDocumentProducer.class);
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
                AnnotationInstance schema = content.value(OPENAPI_RESPONSE_SCHEMA).asNested();
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
            BuildProducer<GeneratedWebResourceBuildItem> resourceBuildItemBuildProducer,
            OpenApiFilteredIndexViewBuildItem openApiFilteredIndexViewBuildItem) throws Exception {

        FilteredIndexView index = openApiFilteredIndexViewBuildItem.getIndex();

        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_OPENAPI));
        OpenAPI staticModel = generateStaticModel(archivesBuildItem);
        OpenAPI annotationModel;
        if (resteasyJaxrsConfig.isPresent()) {
            annotationModel = generateAnnotationModel(index, resteasyJaxrsConfig.get());
        } else {
            annotationModel = null;
        }
        OpenApiDocument finalDocument = loadDocument(staticModel, annotationModel);
        for (OpenApiSerializer.Format format : OpenApiSerializer.Format.values()) {
            String name = OpenApiServlet.GENERATED_DOC_BASE + format;
            resourceBuildItemBuildProducer.produce(new GeneratedWebResourceBuildItem(name,
                    OpenApiSerializer.serialize(finalDocument.get(), format).getBytes(StandardCharsets.UTF_8)));
        }
    }

    @BuildStep
    LogCleanupFilterBuildItem logCleanup() {
        return new LogCleanupFilterBuildItem("io.smallrye.openapi.api.OpenApiDocument",
                "OpenAPI document initialized:");
    }

    private OpenAPI generateStaticModel(ApplicationArchivesBuildItem archivesBuildItem) throws IOException {
        Result result = findStaticModel(archivesBuildItem);
        if (result != null) {
            try (InputStream is = Files.newInputStream(result.path);
                    OpenApiStaticFile staticFile = new OpenApiStaticFile(is, result.format)) {
                return io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile);
            }
        }
        return null;
    }

    private OpenAPI generateAnnotationModel(IndexView indexView, ResteasyJaxrsConfigBuildItem jaxrsConfig) {
        // build a composite index with additional JDK classes, because SmallRye-OpenAPI will check if some
        // app types implement Map and Collection and will go through super classes until Object is reached,
        // and yes, it even checks Object
        // see https://github.com/quarkusio/quarkus/issues/2961
        Indexer indexer = new Indexer();
        Set<DotName> additionalIndex = new HashSet<>();
        IndexingUtil.indexClass(Collection.class.getName(), indexer, indexView, additionalIndex,
                SmallRyeOpenApiProcessor.class.getClassLoader());
        IndexingUtil.indexClass(Map.class.getName(), indexer, indexView, additionalIndex,
                SmallRyeOpenApiProcessor.class.getClassLoader());
        IndexingUtil.indexClass(Object.class.getName(), indexer, indexView, additionalIndex,
                SmallRyeOpenApiProcessor.class.getClassLoader());

        CompositeIndex compositeIndex = CompositeIndex.create(indexView, indexer.complete());

        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);
        return new OpenApiAnnotationScanner(openApiConfig, compositeIndex,
                Collections.singletonList(new RESTEasyExtension(jaxrsConfig, compositeIndex))).scan();
    }

    private Result findStaticModel(ApplicationArchivesBuildItem archivesBuildItem) {
        // Check for the file in both META-INF and WEB-INF/classes/META-INF
        OpenApiSerializer.Format format = OpenApiSerializer.Format.YAML;
        Path resourcePath = archivesBuildItem.getRootArchive().getChildPath(META_INF_OPENAPI_YAML);
        if (resourcePath == null) {
            resourcePath = archivesBuildItem.getRootArchive().getChildPath(WEB_INF_CLASSES_META_INF_OPENAPI_YAML);
        }
        if (resourcePath == null) {
            resourcePath = archivesBuildItem.getRootArchive().getChildPath(META_INF_OPENAPI_YML);
        }
        if (resourcePath == null) {
            resourcePath = archivesBuildItem.getRootArchive().getChildPath(WEB_INF_CLASSES_META_INF_OPENAPI_YML);
        }
        if (resourcePath == null) {
            resourcePath = archivesBuildItem.getRootArchive().getChildPath(META_INF_OPENAPI_JSON);
            format = OpenApiSerializer.Format.JSON;
        }
        if (resourcePath == null) {
            resourcePath = archivesBuildItem.getRootArchive().getChildPath(WEB_INF_CLASSES_META_INF_OPENAPI_JSON);
            format = OpenApiSerializer.Format.JSON;
        }

        if (resourcePath == null) {
            return null;
        }

        return new Result(format, resourcePath);
    }

    static class Result {
        final OpenApiSerializer.Format format;
        final Path path;

        Result(OpenApiSerializer.Format format, Path path) {
            this.format = format;
            this.path = path;
        }
    }

    public OpenApiDocument loadDocument(OpenAPI staticModel, OpenAPI annotationModel) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

        OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig,
                Thread.currentThread().getContextClassLoader());

        OpenApiDocument document = createDocument(openApiConfig);
        if (annotationModel != null) {
            document.modelFromAnnotations(annotationModel);
        }
        document.modelFromReader(readerModel);
        document.modelFromStaticFile(staticModel);
        document.filter(filter(openApiConfig));
        document.initialize();
        return document;
    }

    private OpenApiDocument createDocument(OpenApiConfig openApiConfig) {
        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);
        return document;
    }

    private OASFilter filter(OpenApiConfig openApiConfig) {
        return OpenApiProcessor.getFilter(openApiConfig,
                Thread.currentThread().getContextClassLoader());
    }
}
