package io.quarkus.spring.web.resteasy.reactive.deployment;

import java.util.HashMap;
import java.util.Map;

import jakarta.ws.rs.ext.ParamConverter;

public class MapParamConverter implements ParamConverter<Map<String, String>> {

    @Override
    public Map<String, String> fromString(String value) {
        // Parsear el string de los parámetros a un Map
        Map<String, String> map = new HashMap<>();
        if (value != null && !value.isEmpty()) {
            String[] pairs = value.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    map.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return map;
    }

    @Override
    public String toString(Map<String, String> value) {
        // Convertir el Map a un string
        StringBuilder sb = new StringBuilder();
        value.forEach((key, val) -> sb.append(key).append("=").append(val).append("&"));
        return sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "";
    }
}
