package io.quarkus.vertx.http.runtime.attribute;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.vertx.ext.web.RoutingContext;

/**
 * Query parameter
 */
public class QueryParameterAttribute implements ExchangeAttribute, ExchangeAttributeSerializable {

    private final String parameter;

    private static final String NAME = "Query Parameter";

    public QueryParameterAttribute(String parameter) {
        this.parameter = parameter;
    }

    @Override
    public Map<String, Optional<String>> serialize(RoutingContext exchange) {
        return Map.of(NAME, Optional.ofNullable(this.readAttribute(exchange)));
    }

    @Override
    public String readAttribute(final RoutingContext exchange) {
        List<String> res = exchange.queryParams().getAll(parameter);
        if (res == null) {
            return null;
        } else if (res.isEmpty()) {
            return "";
        } else if (res.size() == 1) {
            return res.get(0);
        } else {
            StringBuilder sb = new StringBuilder("[");
            int i = 0;
            for (String s : res) {
                sb.append(s);
                if (++i != res.size()) {
                    sb.append(", ");
                }
            }
            sb.append("]");
            return sb.toString();
        }
    }

    @Override
    public void writeAttribute(final RoutingContext exchange, final String newValue) throws ReadOnlyAttributeException {
        final ArrayDeque<String> value = new ArrayDeque<>();
        value.add(newValue);
        exchange.queryParams().set(parameter, value);
    }

    public static final class Builder implements ExchangeAttributeBuilder {

        @Override
        public String name() {
            return QueryParameterAttribute.NAME;
        }

        @Override
        public ExchangeAttribute build(final String token) {
            if (token.startsWith("%{q,") && token.endsWith("}")) {
                final String qp = token.substring(4, token.length() - 1);
                return new QueryParameterAttribute(qp);
            }
            return null;
        }

        @Override
        public int priority() {
            return 0;
        }
    }
}
