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

package org.jboss.shamrock.smallrye.openapi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.arc.deployment.AdditionalBeanBuildItem;
import org.jboss.shamrock.arc.deployment.BeanContainerListenerBuildItem;
import org.jboss.shamrock.deployment.annotations.BuildProducer;
import org.jboss.shamrock.deployment.annotations.BuildStep;
import org.jboss.shamrock.deployment.annotations.Record;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.jaxrs.JaxrsConfig;
import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;
import org.jboss.shamrock.smallrye.openapi.runtime.OpenApiDocumentProducer;
import org.jboss.shamrock.smallrye.openapi.runtime.OpenApiServlet;
import org.jboss.shamrock.smallrye.openapi.runtime.SmallRyeOpenApiTemplate;
import org.jboss.shamrock.undertow.ServletBuildItem;

import static org.jboss.shamrock.deployment.annotations.ExecutionTime.STATIC_INIT;

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
                META_INF_OPENAPI_JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON).map(HotDeploymentConfigFileBuildItem::new).collect(Collectors.toList());
    }

    @BuildStep
    ServletBuildItem servlet() {
        ServletBuildItem servletBuildItem = new ServletBuildItem("openapi", OpenApiServlet.class.getName());
        servletBuildItem.getMappings().add(openapi.path);
        return servletBuildItem;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> beans() {
        return Arrays.asList(new AdditionalBeanBuildItem(OpenApiServlet.class),
                new AdditionalBeanBuildItem(OpenApiDocumentProducer.class));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public BeanContainerListenerBuildItem build(SmallRyeOpenApiTemplate template, ApplicationArchivesBuildItem archivesBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<FeatureBuildItem> feature,
	    JaxrsConfig jaxrsConfig) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.SMALLRYE_OPENAPI));
        OpenAPI sm = generateStaticModel(archivesBuildItem);
        OpenAPI am = generateAnnotationModel(combinedIndexBuildItem.getIndex(), jaxrsConfig);
        return new BeanContainerListenerBuildItem(template.setupModel(sm, am));
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

    private OpenAPI generateAnnotationModel(IndexView indexView, JaxrsConfig jaxrsConfig) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);
        return new OpenApiAnnotationScanner(openApiConfig, indexView, Collections.singletonList(new RESTEasyExtension(jaxrsConfig, indexView))).scan();
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
