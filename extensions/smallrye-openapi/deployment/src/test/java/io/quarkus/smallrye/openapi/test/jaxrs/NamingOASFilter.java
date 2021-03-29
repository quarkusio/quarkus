package io.quarkus.smallrye.openapi.test.jaxrs;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;

/**
 * Filter to name the document
 */
public class NamingOASFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        Info info = OASFactory.createInfo();
        info.setTitle("Here my title from a filter");

        openAPI.setInfo(info);
    }

}
