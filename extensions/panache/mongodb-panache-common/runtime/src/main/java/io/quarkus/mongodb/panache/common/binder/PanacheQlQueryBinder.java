package io.quarkus.mongodb.panache.common.binder;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.bson.Document;
import org.bson.conversions.Bson;

import io.quarkus.mongodb.panache.common.runtime.MongoPropertyUtil;
import io.quarkus.panacheql.internal.HqlLexer;
import io.quarkus.panacheql.internal.HqlParser;
import io.quarkus.panacheql.internal.HqlParserBaseVisitor;

public class PanacheQlQueryBinder {

    public static Bson bindQuery(Class<?> clazz, String query, Object[] params) {
        Map<String, String> replacementMap = MongoPropertyUtil.getReplacementMap(clazz);

        //shorthand query
        if (params.length == 1 && query.indexOf('?') == -1) {
            String field = replaceField(query, replacementMap);
            return new Document(field, CommonQueryBinder.paramValue(params[0]));
        }

        //classic query
        Map<String, Object> parameterMaps = new HashMap<>();
        for (int i = 1; i <= params.length; i++) {
            String bindParamsKey = "?" + i;
            parameterMaps.put(bindParamsKey, params[i - 1]);
        }

        return prepareQuery(query, replacementMap, parameterMaps);
    }

    public static Bson bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
        Map<String, String> replacementMap = MongoPropertyUtil.getReplacementMap(clazz);

        Map<String, Object> parameterMaps = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String bindParamsKey = ":" + entry.getKey();
            parameterMaps.put(bindParamsKey, entry.getValue());
        }

        return prepareQuery(query, replacementMap, parameterMaps);
    }

    private static String replaceField(String field, Map<String, String> replacementMap) {
        return replacementMap.getOrDefault(field, field);
    }

    private static Bson prepareQuery(String query, Map<String, String> replacementMap, Map<String, Object> parameterMaps) {
        HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HqlParser parser = new HqlParser(tokens);
        HqlParser.PredicateContext predicate = parser.predicate();
        HqlParserBaseVisitor<Object> visitor = new MongoParserVisitor(replacementMap, parameterMaps);
        return (Bson) predicate.accept(visitor);
    }
}
