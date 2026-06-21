package io.quarkus.mongodb.panache.common.binder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import io.quarkus.panacheql.internal.HqlParser;
import io.quarkus.panacheql.internal.HqlParser.ComparisonPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.GroupedExpressionContext;
import io.quarkus.panacheql.internal.HqlParser.GroupedPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.NamedParameterContext;
import io.quarkus.panacheql.internal.HqlParser.ParameterContext;
import io.quarkus.panacheql.internal.HqlParser.PositionalParameterContext;
import io.quarkus.panacheql.internal.HqlParser.StandardFunctionContext;
import io.quarkus.panacheql.internal.HqlParserBaseVisitor;

class MongoParserVisitor extends HqlParserBaseVisitor<Object> {
    private Map<String, String> replacementMap;
    private Map<String, Object> parameterMaps;

    public MongoParserVisitor(Map<String, String> replacementMap, Map<String, Object> parameterMaps) {
        this.replacementMap = replacementMap;
        this.parameterMaps = parameterMaps;
    }

    @Override
    public Object visitAndPredicate(HqlParser.AndPredicateContext ctx) {
        List<Bson> filters = new ArrayList<>();
        for (HqlParser.PredicateContext predicate : ctx.predicate()) {
            filters.add((Bson) predicate.accept(this));
        }
        return Filters.and(filters);
    }

    @Override
    public Object visitOrPredicate(HqlParser.OrPredicateContext ctx) {
        List<Bson> filters = new ArrayList<>();
        for (HqlParser.PredicateContext predicate : ctx.predicate()) {
            filters.add((Bson) predicate.accept(this));
        }
        return Filters.or(filters);
    }

    @Override
    public Object visitComparisonPredicate(ComparisonPredicateContext ctx) {
        String field = (String) ctx.expression(0).accept(this);
        Object value = ctx.expression(1).accept(this);
        if (ctx.comparisonOperator().EQUAL() != null) {
            return Filters.eq(field, value);
        }
        if (ctx.comparisonOperator().NOT_EQUAL() != null) {
            return Filters.ne(field, value);
        }
        if (ctx.comparisonOperator().GREATER() != null) {
            return Filters.gt(field, value);
        }
        if (ctx.comparisonOperator().GREATER_EQUAL() != null) {
            return Filters.gte(field, value);
        }
        if (ctx.comparisonOperator().LESS() != null) {
            return Filters.lt(field, value);
        }
        if (ctx.comparisonOperator().LESS_EQUAL() != null) {
            return Filters.lte(field, value);
        }
        return super.visitComparisonPredicate(ctx);
    }

    @Override
    public Object visitLikePredicate(HqlParser.LikePredicateContext ctx) {
        String field = (String) ctx.expression(0).accept(this);
        Object parameter = ctx.expression(1).accept(this);
        String pattern = parameter.toString();
        // In case we have something like '/.*/.*' we are in a JavaScript regex, so we must unescape the parameter.
        // We do this here instead of inside visitParameterExpression to avoid unescaping for non-regex parameters.
        if (pattern.startsWith("/") && pattern.lastIndexOf('/') > 0) {
            int lastSlash = pattern.lastIndexOf('/');
            String options = pattern.substring(lastSlash + 1);
            pattern = pattern.substring(1, lastSlash);
            if (!options.isEmpty()) {
                return Filters.regex(field, pattern, options);
            }
        }
        return Filters.regex(field, pattern);
    }

    @Override
    public Object visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
        String field = (String) ctx.expression().accept(this);
        boolean exists = ctx.NOT() != null;
        return Filters.exists(field, exists);
    }

    @Override
    public Object visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
        // FIXME: this only really supports text literals
        String text = ctx.getText();
        if (ctx.literal().STRING_LITERAL() != null) {
            text = text.substring(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public Object visitNamedParameter(NamedParameterContext ctx) {
        return visitParameter(ctx);
    }

    @Override
    public Object visitPositionalParameter(PositionalParameterContext ctx) {
        return visitParameter(ctx);
    }

    @Override
    public Object visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
        return visitParameter(ctx.parameter());
    }

    @Override
    public Object visitGroupedExpression(GroupedExpressionContext ctx) {
        return ctx.expression().accept(this);
    }

    @Override
    public Object visitGroupedPredicate(GroupedPredicateContext ctx) {
        return ctx.predicate().accept(this);
    }

    private Object visitParameter(ParameterContext ctx) {
        // this will match parameters used by PanacheQL : '?1' for index based or ':key' for named one.
        if (parameterMaps.containsKey(ctx.getText())) {
            return CommonQueryBinder.paramValue(parameterMaps.get(ctx.getText()));
        } else {
            // we return the parameter to avoid an exception but the query will be invalid
            return ctx.getText();
        }
    }

    @Override
    public Object visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
        // this is the name of the field, we apply replacement
        String identifier = unquote(ctx.getText());
        return replacementMap.getOrDefault(identifier, identifier);
    }

    /**
     * Removes backticks for quoted identifiers
     */
    private String unquote(String text) {
        if (text.startsWith("`") && text.endsWith("`") && text.length() >= 2) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    @Override
    public Object visitInPredicate(HqlParser.InPredicateContext ctx) {
        String field = (String) ctx.expression().accept(this);
        Object inValue = ctx.inList().accept(this);
        if (inValue instanceof Iterable) {
            return Filters.in(field, (Iterable<?>) inValue);
        } else if (inValue != null && inValue.getClass().isArray()) {
            return Filters.in(field, (Object[]) inValue);
        }
        return Filters.in(field, inValue);
    }

    // Turn new date functions such as instant into regular fields, to not break existing queries
    @Override
    public Object visitStandardFunction(StandardFunctionContext ctx) {
        return ctx.getText();
    }
}
