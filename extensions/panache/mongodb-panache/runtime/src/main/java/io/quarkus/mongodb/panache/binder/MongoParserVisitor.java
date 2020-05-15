package io.quarkus.mongodb.panache.binder;

import java.util.Map;

import io.quarkus.panacheql.internal.HqlParser;
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
    public String visitEqualityPredicate(HqlParser.EqualityPredicateContext ctx) {
        return ctx.expression(0).accept(this) + ":" + ctx.expression(1).accept(this);
    }

    @Override
    public String visitInequalityPredicate(HqlParser.InequalityPredicateContext ctx) {
        return ctx.expression(0).accept(this) + ":{'$ne':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitLessThanOrEqualPredicate(HqlParser.LessThanOrEqualPredicateContext ctx) {
        return ctx.expression(0).accept(this) + ":{'$lte':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitLikePredicate(HqlParser.LikePredicateContext ctx) {
        String parameter = ctx.expression(1).accept(this);
        if (parameter.indexOf('/') == 1 && parameter.lastIndexOf('/') > 1) {
            // In case we have something like '/.*/.*' we are in a JavaScript regex so we must unescape the parameter.
            // We do this here instead of inside visitParameterExpression to avoid unescaping for non-regex parameters.
            parameter = parameter.substring(1, parameter.length() - 1);
        }
        return ctx.expression(0).accept(this) + ":{'$regex':" + parameter + "}";
    }

    @Override
    public String visitGreaterThanPredicate(HqlParser.GreaterThanPredicateContext ctx) {
        return ctx.expression(0).accept(this) + ":{'$gt':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitLessThanPredicate(HqlParser.LessThanPredicateContext ctx) {
        return ctx.expression(0).accept(this) + ":{'$lt':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitGreaterThanOrEqualPredicate(HqlParser.GreaterThanOrEqualPredicateContext ctx) {
        return ctx.expression(0).accept(this) + ":{'$gte':" + ctx.expression(1).accept(this) + "}";
    }

    @Override
    public String visitIsNullPredicate(HqlParser.IsNullPredicateContext ctx) {
        boolean exists = ctx.NOT() != null;
        return ctx.expression().accept(this) + ":{'$exists':" + exists + "}";
    }

    @Override
    public String visitLiteralExpression(HqlParser.LiteralExpressionContext ctx) {
        return CommonQueryBinder.escape(ctx.getText());
    }

    @Override
    public String visitParameterExpression(HqlParser.ParameterExpressionContext ctx) {
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
    public String visitPathExpression(HqlParser.PathExpressionContext ctx) {
        // this is the name of the field, we apply replacement and escape with '
        return "'" + replacementMap.getOrDefault(ctx.getText(), ctx.getText()) + "'";
    }

    @Override
    public String visitInPredicate(HqlParser.InPredicateContext ctx) {
        StringBuilder sb = new StringBuilder(ctx.expression().accept(this))
                .append(":{'$in':[")
                .append(ctx.inList().accept(this))
                .append("]}");
        return sb.toString();
    }
}
