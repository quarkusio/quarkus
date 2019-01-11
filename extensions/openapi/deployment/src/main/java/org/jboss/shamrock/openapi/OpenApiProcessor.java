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

package org.jboss.shamrock.openapi;

import static org.jboss.shamrock.annotations.ExecutionTime.STATIC_INIT;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.IndexView;
import org.jboss.shamrock.annotations.BuildProducer;
import org.jboss.shamrock.annotations.BuildStep;
import org.jboss.shamrock.annotations.Record;
import org.jboss.shamrock.deployment.builditem.AdditionalBeanBuildItem;
import org.jboss.shamrock.deployment.builditem.ApplicationArchivesBuildItem;
import org.jboss.shamrock.deployment.builditem.CombinedIndexBuildItem;
import org.jboss.shamrock.deployment.builditem.FeatureBuildItem;
import org.jboss.shamrock.deployment.builditem.HotDeploymentConfigFileBuildItem;
import org.jboss.shamrock.deployment.cdi.BeanContainerListenerBuildItem;
import org.jboss.shamrock.openapi.runtime.OpenApiDeploymentTemplate;
import org.jboss.shamrock.openapi.runtime.OpenApiDocumentProducer;
import org.jboss.shamrock.openapi.runtime.OpenApiServlet;
import org.jboss.shamrock.undertow.ServletBuildItem;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;

/**
 * @author Ken Finnigan
 */
public class OpenApiProcessor {

    private static final String META_INF_OPENAPI_YAML = "META-INF/openapi.yaml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YAML = "WEB-INF/classes/META-INF/openapi.yaml";
    private static final String META_INF_OPENAPI_YML = "META-INF/openapi.yml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YML = "WEB-INF/classes/META-INF/openapi.yml";
    private static final String META_INF_OPENAPI_JSON = "META-INF/openapi.json";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_JSON = "WEB-INF/classes/META-INF/openapi.json";
    /**
     * The path to register the OpenAPI Servlet
     */
    @ConfigProperty(name = "shamrock.openapi.path", defaultValue = "/openapi")
    String path;

    List<HotDeploymentConfigFileBuildItem> configFiles() {
        return Stream.of(META_INF_OPENAPI_YAML, WEB_INF_CLASSES_META_INF_OPENAPI_YAML,
                META_INF_OPENAPI_YML, WEB_INF_CLASSES_META_INF_OPENAPI_YML,
                META_INF_OPENAPI_JSON, WEB_INF_CLASSES_META_INF_OPENAPI_JSON).map(HotDeploymentConfigFileBuildItem::new).collect(Collectors.toList());
    }

    @BuildStep
    ServletBuildItem servlet() {
        ServletBuildItem servletBuildItem = new ServletBuildItem("openapi", OpenApiServlet.class.getName());
        servletBuildItem.getMappings().add(path);
        return servletBuildItem;
    }

    @BuildStep
    List<AdditionalBeanBuildItem> beans() {
        return Arrays.asList(new AdditionalBeanBuildItem(OpenApiServlet.class),
                new AdditionalBeanBuildItem(OpenApiDocumentProducer.class));
    }

    @BuildStep
    @Record(STATIC_INIT)
    public BeanContainerListenerBuildItem build(OpenApiDeploymentTemplate template, ApplicationArchivesBuildItem archivesBuildItem,
            CombinedIndexBuildItem combinedIndexBuildItem, BuildProducer<FeatureBuildItem> feature) throws Exception {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.MP_OPENAPI));
        Result resourcePath = findStaticModel(archivesBuildItem);
        OpenAPI sm = generateStaticModel(resourcePath == null ? null : resourcePath.path, resourcePath == null ? OpenApiSerializer.Format.YAML : resourcePath.format);
        OpenAPI am = generateAnnotationModel(combinedIndexBuildItem.getIndex());
        return new BeanContainerListenerBuildItem(template.setupModel(sm, am));
    }


    public OpenAPI generateStaticModel(String resourcePath, OpenApiSerializer.Format format) {
        if (resourcePath != null) {
            try (InputStream is = new URL(resourcePath).openStream()) {
                try (OpenApiStaticFile staticFile = new OpenApiStaticFile(is, format)) {
                    return io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Ignore
        }

        return null;
    }

    public OpenAPI generateAnnotationModel(IndexView indexView) {
        Config config = ConfigProvider.getConfig();
        OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);
        return new OpenApiAnnotationScanner(openApiConfig, indexView).scan();
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

        return new Result(format, archivesBuildItem.getRootArchive().getArchiveRoot().relativize(resourcePath).toString());
    }

    static class Result {
        final OpenApiSerializer.Format format;
        final String path;

        Result(OpenApiSerializer.Format format, String path) {
            this.format = format;
            this.path = path;
        }
    }

}
