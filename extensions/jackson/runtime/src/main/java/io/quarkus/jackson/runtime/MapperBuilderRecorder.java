package io.quarkus.jackson.runtime;

import java.util.Optional;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;

import io.quarkus.jackson.MapperBuilderType;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class MapperBuilderRecorder {
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public Supplier<MapperBuilder> supplier(Optional<MapperBuilderType> ot) {
        return ot.map(MapperBuilderType::getSupplier).orElse(() -> new MapperBuilder(new ObjectMapper()) {
        });
    }
}
