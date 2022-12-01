package io.quarkus.smallrye.openapi.test.jaxrs;

import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

/**
 * Filter that count version
 */
public class MyDynamicOASFilter implements OASFilter {

    private static int counter = 0;

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        String version = "3.0." + counter++;
        openAPI.setOpenapi(version);
    }

}
