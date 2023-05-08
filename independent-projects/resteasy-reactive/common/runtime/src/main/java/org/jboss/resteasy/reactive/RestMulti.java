package org.jboss.resteasy.reactive;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.subscription.MultiSubscriber;

/**
 * A wrapper around {@link Multi} that gives resource methods a way to specify the HTTP status code and HTTP headers
 * when streaming a result.
 */
public class RestMulti<T> extends AbstractMulti<T> {

    private final Multi<T> multi;
    private final Integer status;
    private final MultivaluedTreeMap<String, String> headers;

    public static <T> RestMulti.Builder<T> from(Multi<T> multi) {
        return new RestMulti.Builder<>(multi);
    }

    private RestMulti(Builder<T> builder) {
        this.multi = builder.multi;
        this.status = builder.status;
        this.headers = builder.headers;
    }

    @Override
    public void subscribe(MultiSubscriber<? super T> subscriber) {
        multi.subscribe(Infrastructure.onMultiSubscription(multi, subscriber));
    }

    public Integer getStatus() {
        return status;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public static class Builder<T> {
        private final Multi<T> multi;

        private Integer status;

        private final MultivaluedTreeMap<String, String> headers = new CaseInsensitiveMap<>();

        private Builder(Multi<T> multi) {
            this.multi = Objects.requireNonNull(multi, "multi cannot be null");
        }

        public Builder<T> status(int status) {
            this.status = status;
            return this;
        }

        public Builder<T> header(String name, String value) {
            if (value == null) {
                headers.remove(name);
                return this;
            }
            headers.add(name, value);
            return this;
        }

        public RestMulti<T> build() {
            return new RestMulti<>(this);
        }
    }
}
