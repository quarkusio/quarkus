package io.quarkus.mongodb.panache.binder;

import java.util.Map;

public class NativeQueryBinder {

    public static String bindQuery(String query, Object[] params) {
        String bindQuery = query;
        for (int i = 1; i <= params.length; i++) {
            String bindParamsKey = "?" + i;
            bindQuery = CommonQueryBinder.replace(bindQuery, bindParamsKey, params[i - 1]);
        }

        return bindQuery;
    }

    public static String bindQuery(String query, Map<String, Object> params) {
        String bindQuery = query;
        for (Map.Entry entry : params.entrySet()) {
            String bindParamsKey = ":" + entry.getKey();
            bindQuery = CommonQueryBinder.replace(bindQuery, bindParamsKey, entry.getValue());
        }

        return bindQuery;
    }
}
