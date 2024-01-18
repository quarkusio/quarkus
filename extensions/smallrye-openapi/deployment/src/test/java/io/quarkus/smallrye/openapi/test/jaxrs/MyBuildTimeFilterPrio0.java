package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.Optional;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.info.Info;
import org.jboss.jandex.IndexView;

import io.quarkus.smallrye.openapi.OpenApiFilter;

/**
 * Filter to add custom elements
 */
@OpenApiFilter(value = OpenApiFilter.RunStage.BUILD, priority = 0)
public class MyBuildTimeFilterPrio0 implements OASFilter {

    private IndexView view;

    public MyBuildTimeFilterPrio0(IndexView aView) {
        this.view = aView;
    }

    @Override
    public void filterOpenAPI(OpenAPI aOpenAPI) {
        String currentDesc = Optional
                .ofNullable(aOpenAPI.getInfo())
                .map(Info::getDescription)
                .orElse("");
        aOpenAPI.setInfo(OASFactory.createInfo().description(currentDesc + "0"));
    }

}
