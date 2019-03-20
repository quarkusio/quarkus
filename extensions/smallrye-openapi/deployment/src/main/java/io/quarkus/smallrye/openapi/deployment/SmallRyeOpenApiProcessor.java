/*
 * Copyright 2018 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quarkus.smallrye.openapi.deployment;

import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Type;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.BeanContainerListenerBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentConfigFileBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveHierarchyBuildItem;
import io.quarkus.deployment.logging.LogCleanupFilterBuildItem;
import io.quarkus.resteasy.deployment.ResteasyJaxrsConfig;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.smallrye.openapi.runtime.OpenApiDocumentProducer;
import io.quarkus.smallrye.openapi.runtime.OpenApiServlet;
import io.quarkus.smallrye.openapi.runtime.SmallRyeOpenApiTemplate;
import io.quarkus.undertow.deployment.ServletBuildItem;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
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

    @ConfigRoot(name = "smallrye-openapi")
    static final class SmallRyeOpenApiConfig {
        /**
         * The path at which to register the OpenAPI Servlet.
         */
        @ConfigItem(defaultValue = "/openapi")
        String path;
    }

    List<HotDeploymentConfigFileBuildItem> configFiles() {
        return Stream.of(META_INF_OPENAPI_YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML,
                META_INF_OPENAPI_YML, WEB_INF_CLASSES_META_INF_OPENAPI_YML,
                META_INF_OPENAPI_JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON).map(HotDeploymentConfigFileBuildItem::new)
                .collect(Collectors.toList());
    }

    @BuildStep
    ServletBuildItem servlet() {
        return ServletBuildItem.builder("openapi", OpenApiServlet.class.getName())
                .addMapping(openapi.path).build();
    }

    @BuildStep
    List<AdditionalBeanBuildItem> beans() {
        return Arrays.asList(new AdditionalBeanBuildItem(OpenApiServlet.class),
                new AdditionalBeanBuildItem(OpenApiDocumentProducer.class));
    }

    @BuildStep
    public void registerOpenApiSchemaClassesForReflection(BuildProducer<ReflectiveClassBuildItem> reflectiveClass,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchy, CombinedIndexBuildItem combinedIndexBuildItem) {
        IndexView index = combinedIndexBuildItem.getIndex();

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
            AnnotationInstance[] contents = apiResponseAnnotationInstance.value(OPENAPI_RESPONSE_CONTENT).asNestedArray();
            if (contents == null) {
                continue;
            }
            for (AnnotationInstance content : contents) {
                AnnotationInstance schema = content.value(OPENAPI_RESPONSE_SCHEMA).asNested();
                if (schema == null) {
                    continue;
                }

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
    @Record(STATIC_INIT)
    public BeanContainerListenerBuildItem build(SmallRyeOpenApiTemplate template,
            ApplicationArchivesBuildItem archivesBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<FeatureBuildItem> feature,
            ResteasyJaxrsConfig jaxrsConfig) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_OPENAPI));
        OpenAPI sm = generateStaticModel(archivesBuildItem);
        OpenAPI am = generateAnnotationModel(combinedIndexBuildItem.getIndex(), jaxrsConfig);
        return new BeanContainerListenerBuildItem(template.setupModel(sm, am));
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

    private OpenAPI generateAnnotationModel(IndexView indexView, ResteasyJaxrsConfig jaxrsConfig) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);
        return new OpenApiAnnotationScanner(openApiConfig, indexView,
                Collections.singletonList(new RESTEasyExtension(jaxrsConfig, indexView))).scan();
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

}
