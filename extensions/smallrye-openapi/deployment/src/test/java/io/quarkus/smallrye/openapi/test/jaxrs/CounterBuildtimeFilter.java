package io.quarkus.smallrye.openapi.test.jaxrs;

import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;

import io.quarkus.smallrye.openapi.OpenApiFilter;

@OpenApiFilter(OpenApiFilter.RunStage.BUILD)
public class CounterBuildtimeFilter implements OASFilter {

    private static final AtomicInteger TIMES = new AtomicInteger();

    public CounterBuildtimeFilter() {
    }

    @Override
    public void filterOpenAPI(OpenAPI aOpenAPI) {
        int times = TIMES.incrementAndGet();
        aOpenAPI.info(
                OASFactory.createInfo().description("CounterBuildtimeFilter was called " + times + " time(s)"));
    }
}
