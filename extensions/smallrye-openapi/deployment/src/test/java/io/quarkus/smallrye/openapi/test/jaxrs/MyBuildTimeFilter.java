package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.Collection;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.IndexView;

import io.quarkus.smallrye.openapi.OpenApiFilter;

/**
 * Filter to add custom elements
 */
@OpenApiFilter(OpenApiFilter.RunStage.BUILD)
public class MyBuildTimeFilter implements OASFilter {

    private IndexView view;

    public MyBuildTimeFilter(IndexView view) {
        this.view = view;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Collection<ClassInfo> knownClasses = this.view.getKnownClasses();
        Info info = OASFactory.createInfo();
        info.setDescription("Created from Annotated Buildtime filter with " + knownClasses.size() + " known indexed classes");
        openAPI.setInfo(info);
    }

}
