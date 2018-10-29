package org.jboss.shamrock.openapi.runtime;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.jboss.shamrock.runtime.Template;
import org.jboss.shamrock.runtime.cdi.BeanContainer;
import org.jboss.shamrock.runtime.Shamrock;
import org.jboss.shamrock.runtime.cdi.BeanContainerListener;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.OpenApiConfigImpl;
import io.smallrye.openapi.api.OpenApiDocument;
import io.smallrye.openapi.runtime.OpenApiProcessor;

/**
 * @author Ken Finnigan
 */
@Template
public class OpenApiDeploymentTemplate {

    public BeanContainerListener setupModel(OpenAPI staticModel, OpenAPI annotationModel) {
        return new BeanContainerListener() {
            @Override
            public void created(BeanContainer container) {
                Config config = ConfigProvider.getConfig();
                OpenApiConfig openApiConfig = new OpenApiConfigImpl(config);

                OpenAPI readerModel = OpenApiProcessor.modelFromReader(openApiConfig, Shamrock.class.getClassLoader());

                OpenApiDocument document = createDocument(openApiConfig);
                document.modelFromAnnotations(annotationModel);
                document.modelFromReader(readerModel);
                document.modelFromStaticFile(staticModel);
                document.filter(filter(openApiConfig));
                document.initialize();
                container.instance(OpenApiDocumentProducer.class).setDocument(document);
            }
        };
    }

    private OpenApiDocument createDocument(OpenApiConfig openApiConfig) {
        OpenApiDocument document = OpenApiDocument.INSTANCE;
        document.reset();
        document.config(openApiConfig);
        return document;
    }

    private OASFilter filter(OpenApiConfig openApiConfig) {
        return OpenApiProcessor.getFilter(openApiConfig, Shamrock.class.getClassLoader());
    }
}
