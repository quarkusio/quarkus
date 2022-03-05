package io.quarkus.jackson.runtime;

import java.time.ZoneId;
import java.util.TimeZone;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

import io.quarkus.arc.DefaultBean;
import io.quarkus.jackson.JsonMapperCustomizer;
import io.quarkus.jackson.ObjectMapperCustomizer;

@ApplicationScoped
public class JsonMapperProducer {

    private static final Logger log = Logger.getLogger(JsonMapperProducer.class);

    private static void configureBuilder(Instance<ObjectMapperCustomizer> objectMapperCustomizers, JsonMapper.Builder builder) {
        objectMapperCustomizers.stream().sorted().forEach(
                c -> {
                    if (c instanceof JsonMapperCustomizer) {
                        ((JsonMapperCustomizer) c).customize(builder);
                    }
                });
    }

    // transitional backwards compatibility
    private static void customizeObjectMapper(Instance<ObjectMapperCustomizer> objectMapperCustomizers, JsonMapper jsonMapper) {
        objectMapperCustomizers.stream().sorted().forEach(
                c -> {
                    if (!(c instanceof JsonMapperCustomizer)) {
                        log.warnv("io.quarkus.jackson.ObjectMapperCustomizer has been deprecated; Update {0}",
                                c.getClass().getName());
                        c.customize(jsonMapper);
                    }
                });
    }

    @DefaultBean
    @Singleton
    @Produces
    public JsonMapper jsonMapper(JacksonBuildTimeConfig jacksonBuildTimeConfig,
            Instance<ObjectMapperCustomizer> objectMapperCustomizers) {

        JsonMapper.Builder builder = JsonMapper.builder(new MappingJsonFactory());
        if (!jacksonBuildTimeConfig.failOnUnknownProperties) {
            // this feature is enabled by default, so we disable it
            builder.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        }
        if (!jacksonBuildTimeConfig.failOnEmptyBeans) {
            // this feature is enabled by default, so we disable it
            builder.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        }
        if (!jacksonBuildTimeConfig.writeDatesAsTimestamps) {
            // this feature is enabled by default, so we disable it
            builder.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        if (jacksonBuildTimeConfig.acceptCaseInsensitiveEnums) {
            builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
        }
        jacksonBuildTimeConfig.serializationInclusion.ifPresent(builder::serializationInclusion);

        ZoneId zoneId = jacksonBuildTimeConfig.timezone.orElse(null);
        if ((zoneId != null) && !zoneId.getId().equals("UTC")) { // Jackson uses UTC as the default, so let's not reset it
            builder.defaultTimeZone(TimeZone.getTimeZone(zoneId));
        }

        configureBuilder(objectMapperCustomizers, builder);

        JsonMapper jsonMapper = builder.build();
        // Unfortunately, backwards compatibility may have lower priority ObjectMapperCustomizer(s) override JsonMapperCustomizer(s)
        customizeObjectMapper(objectMapperCustomizers, jsonMapper);
        return jsonMapper;
    }
}
