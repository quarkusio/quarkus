package io.quarkus.mongodb.panache.common.binder;

import java.util.Map;

import io.quarkus.panacheql.internal.HqlParser;
import io.quarkus.panacheql.internal.HqlParser.ComparisonPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.GroupedExpressionContext;
import io.quarkus.panacheql.internal.HqlParser.GroupedPredicateContext;
import io.quarkus.panacheql.internal.HqlParser.NamedParameterContext;
import io.quarkus.panacheql.internal.HqlParser.ParameterContext;
import io.quarkus.panacheql.internal.HqlParser.PositionalParameterContext;
import io.quarkus.panacheql.internal.HqlParser.StandardFunctionContext;
import io.quarkus.panacheql.internal.HqlParserBaseVisitor;

class MongoParserVisitor extends HqlParserBaseVisitor<String> {
    private Map<String, String> replacementMap;
    private Map<String, Object> parameterMaps;

    public MongoParserVisitor(Map<String, String> replacementMap, Map<String, Object> parameterMaps) {
        this.replacementMap = replacementMap;
        this.parameterMaps = parameterMaps;
    }

    @Override
    public String visitAndPredicate(HqlParser.AndPredicateContext ctx) {
        StringBuilder sb = new StringBuilder();
        for (HqlParser.PredicateContext predicate : ctx.predicate()) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(predicate.accept(this));
        }
        return sb.toString();
    }

    @Override
    public String visitOrPredicate(HqlParser.OrPredicateContext ctx) {
        StringBuilder sb = new StringBuilder("'$or':[");
        for (HqlParser.PredicateContext predicate : ctx.predicate()) {
            if (sb.length() > 7)
                sb.append(",");
            sb.append('{').append(predicate.accept(this)).append('}');
        }
        sb.append("]");
        return sb.toString();
    }

    @Override
    public String visitComparisonPredicate(ComparisonPredicateContext ctx) {
        String lhs = ctx.expression(0).accept(this);
        String rhs = ctx.expression(1).accept(this);
        if (ctx.comparisonOperator().EQUAL() != null) {
            return lhs + ":" + rhs;
        }
        if (ctx.comparisonOperator().NOT_EQUAL() != null) {
            return lhs + ":{'$ne':" + rhs + "}";
        }
        if (ctx.comparisonOperator().GREATER() != null) {
            return lhs + ":{'$gt':" + rhs + "}";
        }
        if (ctx.comparisonOperator().GREATER_EQUAL() != null) {
            return lhs + ":{'$gte':" + rhs + "}";
        }
        if (ctx.comparisonOperator().LESS() != null) {
            return lhs + ":{'$lt':" + rhs + "}";
        }
        if (ctx.comparisonOperator().LESS_EQUAL() != null) {
            return lhs + ":{'$lte':" + rhs + "}";
        }
        return super.visitComparisonPredicate(ctx);
    }

    @Override
    public String visitLikePredicate(HqlParser.LikePredicateContext ctx) {
        String parameter = ctx.expression(1).accept(this);
        if (parameter.indexOf('/') == 1 && parameter.lastIndexOf('/') > 1) {
            // In case we have something like '/.*/.*' we are in a JavaScript regex, so we must unescape the parameter.
            // We do this here instead of inside visitParameterExpression to avoid unescaping for non-regex parameters.
            parameter = parameter.substring(1, parameter.length() - 1);
        }
        return ctx.expression(0).accept(this) + ":{'$regex':" + parameter + "}";
    }

    @Override
    public String visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
        boolean exists = ctx.NOT() != null;
        return ctx.expression().accept(this) + ":{'$exists':" + exists + "}";
    }

    @Override
    public String visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
        String text = ctx.getText();
        // FIXME: this only really supports text literals
        if (ctx.literal().STRING_LITERAL() != null) {
            text = text.substring(1, text.length() - 1);
        }
        return CommonQueryBinder.escape(text);
    }

    @Override
    public String visitNamedParameter(NamedParameterContext ctx) {
        return visitParameter(ctx);
    }

    @Override
    public String visitPositionalParameter(PositionalParameterContext ctx) {
        return visitParameter(ctx);
    }

    @Override
    public String visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
        return visitParameter(ctx.parameter());
    }

    @Override
    public String visitGroupedExpression(GroupedExpressionContext ctx) {
        return ctx.expression().accept(this);
    }

    @Override
    public String visitGroupedPredicate(GroupedPredicateContext ctx) {
        return ctx.predicate().accept(this);
    }

    private String visitParameter(ParameterContext ctx) {
        // this will match parameters used by PanacheQL : '?1' for index based or ':key' for named one.
        if (parameterMaps.containsKey(ctx.getText())) {
            Object value = parameterMaps.get(ctx.getText());
            return CommonQueryBinder.escape(value);
        } else {
            // we return the parameter to avoid an exception but the query will be invalid
            return ctx.getText();
        }
    }

    @Override
    public String visitGeneralPathExpression(HqlParser.GeneralPathExpressionContext ctx) {
        String identifier = unquote(ctx.getText());
        // this is the name of the field, we apply replacement and escape with '
        return "'" + replacementMap.getOrDefault(identifier, identifier) + "'";
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
    public String visitInPredicate(HqlParser.InPredicateContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.expression().accept(this))
                .append(":{'$in':")
                .append(ctx.inList().accept(this))
                .append("}");
        return sb.toString();
    }

    // Turn new date functions such as instant into regular fields, to not break existing queries
    @Override
    public String visitStandardFunction(StandardFunctionContext ctx) {
        return "'" + ctx.getText() + "'";
    }
}
