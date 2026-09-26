package io.quarkus.smallrye.openapi.runtime.graal;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

import io.smallrye.openapi.api.OpenApiConfig;
import io.smallrye.openapi.api.SmallRyeOpenAPI;
import io.smallrye.openapi.runtime.io.JsonIO;

@TargetClass(className = "io.smallrye.openapi.runtime.io.Jackson2JsonIO")
@Delete
final class Target_io_smallrye_openapi_runtime_io_Jackson2JsonIO {
}

@TargetClass(className = "io.smallrye.openapi.runtime.io.JakartaJsonIO")
@Delete
final class Target_io_smallrye_openapi_runtime_io_JakartaJsonIO {
}

@TargetClass(className = "io.smallrye.openapi.runtime.io.Jackson3JsonIO")
final class Target_io_smallrye_openapi_runtime_io_Jackson3JsonIO {

    @Alias
    Target_io_smallrye_openapi_runtime_io_Jackson3JsonIO(OpenApiConfig config) {
    }
}

@TargetClass(JsonIO.class)
final class Target_io_smallrye_openapi_runtime_io_JsonIO {

    @Substitute
    @SuppressWarnings("rawtypes")
    private static JsonIO newInstanceRaw(SmallRyeOpenAPI.JsonProvider jsonProvider, OpenApiConfig config) {
        return (JsonIO) (Object) new Target_io_smallrye_openapi_runtime_io_Jackson3JsonIO(config);
    }
}

public class SmallRyeOpenApiSubstitutions {
}
