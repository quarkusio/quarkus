package io.smallrye.reactive.kafka.graal;

import java.util.function.BooleanSupplier;

import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;
import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Delete;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

final class HasStrimzi implements BooleanSupplier {

    @Override
    public boolean getAsBoolean() {
        try {
            KafkaSubstitutions.class.getClassLoader()
                    .loadClass("io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

@TargetClass(className = "com.jayway.jsonpath.internal.filter.ValueNodes", innerClass = "JsonNode", onlyWith = HasStrimzi.class)
final class Target_com_jayway_jsonpath_internal_filter_ValueNodes_JsonNode {
    @Alias
    private Object json;
    @Alias
    private boolean parsed;

    @Substitute
    public Object parse(Predicate.PredicateContext ctx) {
        try {
            return parsed ? json : new JacksonJsonProvider().parse(json.toString());
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
    }
}

@TargetClass(className = "com.jayway.jsonpath.internal.filter.ValueNode", onlyWith = HasStrimzi.class)
final class Target_com_jayway_jsonpath_internal_filter_ValueNode {

    @Substitute
    private static boolean isJson(Object o) {
        if (o == null || !(o instanceof String)) {
            return false;
        }
        String str = o.toString().trim();
        if (str.length() <= 1) {
            return false;
        }
        char c0 = str.charAt(0);
        char c1 = str.charAt(str.length() - 1);
        if ((c0 == '[' && c1 == ']') || (c0 == '{' && c1 == '}')) {
            try {
                new JacksonJsonProvider().parse(str);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }
}

@TargetClass(className = "com.jayway.jsonpath.internal.DefaultsImpl", onlyWith = HasStrimzi.class)
final class Target_com_jayway_jsonpath_internal_DefaultsImpl {
    @Delete // Delete the no longer used mappingProvider
    private MappingProvider mappingProvider;

    @Substitute
    public JsonProvider jsonProvider() {
        return new JacksonJsonNodeJsonProvider();
    }

    @Substitute
    public MappingProvider mappingProvider() {
        return new JacksonMappingProvider();
    }
}
