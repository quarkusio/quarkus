package io.quarkus.smallrye.openapi.deployment.filter;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;

public class DefaultInfoFilter implements OASFilter {

    final Config config;

    public DefaultInfoFilter(Config config) {
        this.config = config;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Info info = openAPI.getInfo();

        if (info == null) {
            info = OASFactory.createInfo();
            openAPI.setInfo(info);
        }

        if (info.getTitle() == null) {
            String title = config.getOptionalValue("quarkus.application.name", String.class).orElse("Generated");
            info.setTitle(title + " API");
        }
        if (info.getVersion() == null) {
            String version = config.getOptionalValue("quarkus.application.version", String.class).orElse("1.0");
            info.setVersion((version == null ? "1.0" : version));
        }
    }
}
