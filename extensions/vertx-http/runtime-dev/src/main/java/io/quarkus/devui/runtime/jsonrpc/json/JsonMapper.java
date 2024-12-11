package io.quarkus.devui.runtime.jsonrpc.json;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import io.quarkus.dev.console.DeploymentLinker;

public interface JsonMapper {

    String toString(Object object, boolean pretty);

    <T> T fromString(String json, Class<T> target);

    <T> T fromValue(Object json, Class<T> target);

    static DeploymentLinker<JsonMapper> deploymentLinker() {
        return new DeploymentLinker<>() {
            @Override
            public Map<String, ?> createLinkData(JsonMapper object) {
                return Map.of("delegate", object,
                        "toString", (BiFunction<Object, Boolean, String>) object::toString,
                        "fromString", (BiFunction<String, Class<?>, Object>) object::fromString,
                        "fromValue", (BiFunction<Object, Class<?>, Object>) object::fromValue);
            }

            @Override
            @SuppressWarnings("unchecked")
            public JsonMapper createLink(Map<String, ?> linkData) {
                Object delegate = linkData.get("delegate");
                BiFunction<Object, Boolean, String> toString = (BiFunction<Object, Boolean, String>) linkData.get("toString");
                BiFunction<Object, Class<?>, Object> fromString = (BiFunction<Object, Class<?>, Object>) linkData
                        .get("fromString");
                BiFunction<Object, Class<?>, Object> fromValue = (BiFunction<Object, Class<?>, Object>) linkData
                        .get("fromValue");
                return new JsonMapper() {
                    @Override
                    public String toString() {
                        return "JsonMapper[delegate=" + delegate + "]";
                    }

                    @Override
                    public String toString(Object object, boolean pretty) {
                        return toString.apply(object, pretty);
                    }

                    @Override
                    public <T> T fromString(String json, Class<T> target) {
                        return target.cast(fromString.apply(json, target));
                    }

                    @Override
                    public <T> T fromValue(Object json, Class<T> target) {
                        if (target.isPrimitive()) {
                            return (T) json;
                        } else {
                            return target.cast(fromValue.apply(json, target));
                        }
                    }
                };
            }
        };
    }

    interface Factory {

        /**
         * Creates the mapper, delegating to the deployment to configure and implement it.
         * <p>
         * We can't implement it in the runtime because we don't have a dependency to Jackson in the runtime.
         *
         * @return A JSON mapper implemented in the deployment module.
         */
        JsonMapper create(JsonTypeAdapter<?, Map<String, Object>> jsonObjectAdapter,
                JsonTypeAdapter<?, List<?>> jsonArrayAdapter,
                JsonTypeAdapter<?, String> bufferAdapter);

        static DeploymentLinker<Factory> deploymentLinker() {
            return new DeploymentLinker<>() {
                @Override
                public Map<String, ?> createLinkData(Factory object) {
                    return Map.of(
                            "delegate", object,
                            "create",
                            (Function<Map<String, ?>, Map<String, ?>>) args -> {
                                var created = object.create(typeAdapterFromLinkData(args.get("jsonObjectAdapter")),
                                        typeAdapterFromLinkData(args.get("jsonArrayAdapter")),
                                        typeAdapterFromLinkData(args.get("bufferAdapter")));
                                return JsonMapper.deploymentLinker().createLinkData(created);
                            });
                }

                @Override
                @SuppressWarnings("unchecked")
                public Factory createLink(Map<String, ?> linkData) {
                    Object delegate = linkData.get("delegate");
                    Function<Map<String, ?>, Map<String, ?>> create = (Function<Map<String, ?>, Map<String, ?>>) linkData
                            .get("create");
                    return new Factory() {
                        @Override
                        public String toString() {
                            return "JsonMapper[delegate=" + delegate + "]";
                        }

                        @Override
                        public JsonMapper create(JsonTypeAdapter<?, Map<String, Object>> jsonObjectAdapter,
                                JsonTypeAdapter<?, List<?>> jsonArrayAdapter,
                                JsonTypeAdapter<?, String> bufferAdapter) {
                            var linkData = create.apply(Map.of("jsonObjectAdapter", typeAdapterToLinkData(jsonObjectAdapter),
                                    "jsonArrayAdapter", typeAdapterToLinkData(jsonArrayAdapter),
                                    "bufferAdapter", typeAdapterToLinkData(bufferAdapter)));
                            return JsonMapper.deploymentLinker().createLink(linkData);
                        }
                    };
                }

                private Map<String, ?> typeAdapterToLinkData(JsonTypeAdapter<?, ?> object) {
                    return Map.of("type", object.type,
                            "serializer", object.serializer,
                            "deserializer", object.deserializer);
                }

                @SuppressWarnings({ "unchecked", "rawtypes" })
                private <T, S> JsonTypeAdapter<T, S> typeAdapterFromLinkData(Object linkData) {
                    Map<String, ?> map = (Map<String, ?>) linkData;
                    return new JsonTypeAdapter<>((Class) map.get("type"),
                            (Function) map.get("serializer"),
                            (Function) map.get("deserializer"));
                }
            };
        }
    }

}
