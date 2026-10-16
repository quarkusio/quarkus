package io.quarkus.mongodb.panache.common.binder;

import java.util.HashMap;
import java.util.Map;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.bson.Document;
import org.bson.conversions.Bson;

import io.quarkus.mongodb.panache.common.runtime.MongoPropertyUtil;
import io.quarkus.panache.common.exception.PanacheQueryException;
import io.quarkus.panacheql.internal.HqlLexer;
import io.quarkus.panacheql.internal.HqlParser;

public class PanacheQlQueryBinder {

    public static Bson bindQuery(Class<?> clazz, String query, Object[] params) {
        return bind(clazz, query, params, false);
    }

    public static Bson bindQuery(Class<?> clazz, String query, Map<String, Object> params) {
        return bind(clazz, query, params, false);
    }

    /**
     * Binds a PanacheQL update like <code>firstname = ?1 and status = ?2</code> to the fields to set:
     * <code>{'firstname': ?1, 'status': ?2}</code>.
     */
    public static Bson bindUpdate(Class<?> clazz, String update, Object[] params) {
        return bind(clazz, update, params, true);
    }

    /**
     * Binds a PanacheQL update like <code>firstname = :firstname and status = :status</code> to the fields to set:
     * <code>{'firstname': :firstname, 'status': :status}</code>.
     */
    public static Bson bindUpdate(Class<?> clazz, String update, Map<String, Object> params) {
        return bind(clazz, update, params, true);
    }

    private static Bson bind(Class<?> clazz, String query, Object[] params, boolean update) {
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

        return prepareQuery(query, replacementMap, parameterMaps, update);
    }

    private static Bson bind(Class<?> clazz, String query, Map<String, Object> params, boolean update) {
        Map<String, String> replacementMap = MongoPropertyUtil.getReplacementMap(clazz);

        Map<String, Object> parameterMaps = new HashMap<>();
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String bindParamsKey = ":" + entry.getKey();
            parameterMaps.put(bindParamsKey, entry.getValue());
        }

        return prepareQuery(query, replacementMap, parameterMaps, update);
    }

    private static String replaceField(String field, Map<String, String> replacementMap) {
        return replacementMap.getOrDefault(field, field);
    }

    private static Bson prepareQuery(String query, Map<String, String> replacementMap, Map<String, Object> parameterMaps,
            boolean update) {
        HqlLexer lexer = new HqlLexer(CharStreams.fromString(query));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        HqlParser parser = new HqlParser(tokens);
        HqlParser.PredicateContext predicate = parser.predicate();
        MongoParserVisitor visitor = new MongoParserVisitor(replacementMap, parameterMaps, update);
        if (!update) {
            return (Bson) predicate.accept(visitor);
        }
        // the parser stops at the first token it can't use, e.g. the comma of 'field1 = ?1, field2 = ?2',
        // which would otherwise silently drop the rest of the update
        if (parser.getCurrentToken().getType() != Token.EOF) {
            throw new PanacheQueryException("Unexpected '" + parser.getCurrentToken().getText() + "' in the update '" + query
                    + "', the fields to set must be separated by 'and'");
        }
        return visitor.visitAssignments(predicate);
    }
}
