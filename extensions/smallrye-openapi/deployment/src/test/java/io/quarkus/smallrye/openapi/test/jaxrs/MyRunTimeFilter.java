package io.quarkus.smallrye.openapi.test.jaxrs;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;

import io.quarkus.smallrye.openapi.OpenApiFilter;

/**
 * Filter to add custom elements
 */
@OpenApiFilter(OpenApiFilter.RunStage.RUN)
public class MyRunTimeFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Info info = OASFactory.createInfo();
        info.setDescription("Created from Annotated Runtime filter");
        openAPI.setInfo(info);
    }

}
