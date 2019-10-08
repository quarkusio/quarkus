package io.quarkus.smallrye.openapi.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.jandex.CompositeIndex;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.Indexer;

import io.quarkus.deployment.builditem.ApplicationArchivesBuildItem;
import io.quarkus.deployment.index.IndexingUtil;
import io.quarkus.resteasy.deployment.ResteasyJaxrsConfigBuildItem;
import io.quarkus.smallrye.openapi.runtime.OpenApiHandler;
import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;
import io.smallrye.openapi.runtime.OpenApiStaticFile;
import io.smallrye.openapi.runtime.io.OpenApiSerializer;
import io.smallrye.openapi.runtime.scanner.OpenApiAnnotationScanner;

public class OpenApiScanningUtil {

    private static final String META_INF_OPENAPI_YAML = "META-INF/openapi.yaml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YAML = "WEB-INF/classes/META-INF/openapi.yaml";
    private static final String META_INF_OPENAPI_YML = "META-INF/openapi.yml";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_YML = "WEB-INF/classes/META-INF/openapi.yml";
    private static final String META_INF_OPENAPI_JSON = "META-INF/openapi.json";
    private static final String WEB_INF_CLASSES_META_INF_OPENAPI_JSON = "WEB-INF/classes/META-INF/openapi.json";

    public static OpenAPI generateStaticModel(ApplicationArchivesBuildItem archivesBuildItem) throws IOException {
        Result result = findStaticModel(archivesBuildItem);
        if (result != null) {
            try (InputStream is = Files.newInputStream(result.path);
                    OpenApiStaticFile staticFile = new OpenApiStaticFile(is, result.format)) {
                return io.smallrye.openapi.runtime.OpenApiProcessor.modelFromStaticFile(staticFile);
            }
        }
        return null;
    }

    public static OpenAPI generateAnnotationModel(IndexView indexView, ResteasyJaxrsConfigBuildItem jaxrsConfig) {
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

    private static Result findStaticModel(ApplicationArchivesBuildItem archivesBuildItem) {
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

    public static class Result {
        final OpenApiSerializer.Format format;
        final Path path;

        public Result(OpenApiSerializer.Format format, Path path) {
            this.format = format;
            this.path = path;
        }
    }

    public static Map<String, byte[]> loadDocument(OpenAPI staticModel, OpenAPI annotationModel) throws IOException {
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
        Map<String, byte[]> ret = new HashMap<>();

        for (OpenApiSerializer.Format format : OpenApiSerializer.Format.values()) {
            String name = OpenApiHandler.BASE_NAME + format;
            ret.put(name, OpenApiSerializer.serialize(document.get(), format).getBytes(StandardCharsets.UTF_8));
        }
        return ret;
    }

    private static OpenApiDocument createDocument(OpenApiConfig openApiConfig) {
        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);
        return document;
    }

    private static OASFilter filter(OpenApiConfig openApiConfig) {
        return OpenApiProcessor.getFilter(openApiConfig,
                Thread.currentThread().getContextClassLoader());
    }
}
