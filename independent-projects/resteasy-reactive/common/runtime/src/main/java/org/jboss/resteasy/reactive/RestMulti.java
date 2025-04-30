package org.jboss.resteasy.reactive;

import static io.smallrye.mutiny.helpers.ParameterValidation.MAPPER_RETURNED_NULL;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongFunction;

import org.jboss.resteasy.reactive.common.util.CaseInsensitiveMap;
import org.jboss.resteasy.reactive.common.util.MultivaluedTreeMap;

import io.smallrye.mutiny.Context;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.groups.MultiMerge;
import io.smallrye.mutiny.helpers.EmptyUniSubscription;
import io.smallrye.mutiny.helpers.Subscriptions;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import io.smallrye.mutiny.operators.AbstractMulti;
import io.smallrye.mutiny.operators.AbstractUni;
import io.smallrye.mutiny.subscription.ContextSupport;
import io.smallrye.mutiny.subscription.MultiSubscriber;
import io.smallrye.mutiny.subscription.UniSubscriber;
import io.smallrye.mutiny.subscription.UniSubscription;

/**
 * A wrapper around {@link Multi} that gives resource methods a way to specify the HTTP status code and HTTP headers
 * when streaming a result.
 */
@SuppressWarnings("ReactiveStreamsUnusedPublisher")
public abstract class RestMulti<T> extends AbstractMulti<T> {

    public abstract Integer getStatus();

    public abstract Map<String, List<String>> getHeaders();

    public static <T> RestMulti.SyncRestMulti.Builder<T> fromMultiData(Multi<T> multi) {
        return new RestMulti.SyncRestMulti.Builder<>(multi);
    }

    public static <T, R> RestMulti<R> fromUniResponse(Uni<T> uni,
            Function<T, Multi<R>> dataExtractor) {
        return fromUniResponse(uni, dataExtractor, null, null);
    }

    public static <T, R> RestMulti<R> fromUniResponse(Uni<T> uni,
            Function<T, Multi<R>> dataExtractor,
            Function<T, Map<String, List<String>>> headersExtractor) {
        return fromUniResponse(uni, dataExtractor, headersExtractor, null);
    }

    public static <T, R> RestMulti<R> fromUniResponse(Uni<T> uni,
            Function<T, Multi<R>> dataExtractor,
            Function<T, Map<String, List<String>>> headersExtractor,
            Function<T, Integer> statusExtractor) {
        Function<? super T, ? extends Multi<? extends R>> actualDataExtractor = Infrastructure
                .decorate(nonNull(dataExtractor, "dataExtractor"));
        return (RestMulti<R>) Infrastructure.onMultiCreation(new AsyncRestMulti<>(uni, actualDataExtractor,
                headersExtractor, statusExtractor));
    }

    public static class SyncRestMulti<T> extends RestMulti<T> {

        private final Multi<T> multi;
        private final Integer status;
        private final MultivaluedTreeMap<String, String> headers;
        private final long demand;
        private final boolean encodeAsJsonArray;

        @Override
        public void subscribe(MultiSubscriber<? super T> subscriber) {
            multi.subscribe(Infrastructure.onMultiSubscription(multi, subscriber));
        }

        private SyncRestMulti(Builder<T> builder) {
            this.multi = builder.multi;
            this.status = builder.status;
            this.headers = builder.headers;
            this.demand = builder.demand;
            this.encodeAsJsonArray = builder.encodeAsJsonArray;
        }

        @Override
        public Integer getStatus() {
            return status;
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public long getDemand() {
            return demand;
        }

        public boolean encodeAsJsonArray() {
            return encodeAsJsonArray;
        }

        public static class Builder<T> {
            private final Multi<T> multi;
            private final MultivaluedTreeMap<String, String> headers = new CaseInsensitiveMap<>();
            private Integer status;
            private long demand = 1;
            private boolean encodeAsJsonArray = true;

            private Builder(Multi<T> multi) {
                this.multi = Objects.requireNonNull(multi, "multi cannot be null");
            }

            /**
             * Configure the {@code demand} signaled to the wrapped {@link Multi}, defaults to {@code 1}.
             *
             * <p>
             * A demand of {@code 1} guarantees serial/sequential processing, any higher demand supports
             * concurrent processing. A demand greater {@code 1}, with concurrent {@link Multi} processing,
             * does not guarantee element order - this means that elements emitted by the
             * {@link RestMulti#fromMultiData(Multi) RestMulti.fromMultiData(Multi)} source <code>Multi</code>}
             * will be produced in a non-deterministic order.
             *
             * @see MultiMerge#withConcurrency(int) Multi.createBy().merging().withConcurrency(int)
             * @see Multi#capDemandsTo(long)
             * @see Multi#capDemandsUsing(LongFunction)
             */
            public Builder<T> withDemand(long demand) {
                if (demand <= 0) {
                    throw new IllegalArgumentException("Demand must be greater than zero");
                }
                this.demand = demand;
                return this;
            }

            /**
             * Configure whether objects produced by the wrapped {@link Multi} are encoded as JSON array elements, which is the
             * default.
             *
             * <p>
             * {@code encodeAsJsonArray(false)} produces separate JSON objects.
             *
             * <p>
             * This property is only used for JSON object results and ignored for SSE and chunked streaming.
             */
            public Builder<T> encodeAsJsonArray(boolean encodeAsJsonArray) {
                this.encodeAsJsonArray = encodeAsJsonArray;
                return this;
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
                return new SyncRestMulti<>(this);
            }
        }
    }

    // Copied from: io.smallrye.mutiny.operators.uni.UniOnItemTransformToUni while adding header and status extraction
    public static class AsyncRestMulti<I, O> extends RestMulti<O> {

        private final Function<? super I, ? extends Multi<? extends O>> dataExtractor;
        private final Function<I, Integer> statusExtractor;
        private final Function<I, Map<String, List<String>>> headersExtractor;
        private final AtomicReference<Integer> status;
        private final AtomicReference<Map<String, List<String>>> headers;
        private final Uni<I> upstream;

        public <T> AsyncRestMulti(Uni<I> upstream,
                Function<? super I, ? extends Multi<? extends O>> dataExtractor,
                Function<I, Map<String, List<String>>> headersExtractor,
                Function<I, Integer> statusExtractor) {
            this.upstream = upstream;
            this.dataExtractor = dataExtractor;
            this.statusExtractor = statusExtractor;
            this.headersExtractor = headersExtractor;
            this.status = new AtomicReference<>(null);
            this.headers = new AtomicReference<>(Collections.emptyMap());
        }

        @Override
        public void subscribe(MultiSubscriber<? super O> subscriber) {
            if (subscriber == null) {
                throw new NullPointerException("The subscriber must not be `null`");
            }
            AbstractUni.subscribe(upstream, new FlatMapPublisherSubscriber<>(subscriber, dataExtractor, statusExtractor, status,
                    headersExtractor, headers));
        }

        @Override
        public Integer getStatus() {
            return status.get();
        }

        @Override
        public Map<String, List<String>> getHeaders() {
            return headers.get();
        }

        static final class FlatMapPublisherSubscriber<I, O>
                implements Flow.Subscriber<O>, UniSubscriber<I>, Flow.Subscription, ContextSupport {

            private final AtomicReference<Flow.Subscription> secondUpstream;
            private final AtomicReference<UniSubscription> firstUpstream;
            private final Flow.Subscriber<? super O> downstream;
            private final Function<? super I, ? extends Multi<? extends O>> dataExtractor;
            private final Function<I, Integer> statusExtractor;
            private final AtomicReference<Integer> status;
            private final Function<I, Map<String, List<String>>> headersExtractor;
            private final AtomicReference<Map<String, List<String>>> headers;
            private final AtomicLong requested = new AtomicLong();

            public FlatMapPublisherSubscriber(Flow.Subscriber<? super O> downstream,
                    Function<? super I, ? extends Multi<? extends O>> dataExtractor,
                    Function<I, Integer> statusExtractor,
                    AtomicReference<Integer> status,
                    Function<I, Map<String, List<String>>> headersExtractor,
                    AtomicReference<Map<String, List<String>>> headers) {
                this.downstream = downstream;
                this.dataExtractor = dataExtractor;
                this.statusExtractor = statusExtractor;
                this.status = status;
                this.headersExtractor = headersExtractor;
                this.headers = headers;
                this.firstUpstream = new AtomicReference<>();
                this.secondUpstream = new AtomicReference<>();
            }

            @Override
            public void onNext(O item) {
                downstream.onNext(item);
            }

            @Override
            public void onError(Throwable failure) {
                downstream.onError(failure);
            }

            @Override
            public void onComplete() {
                downstream.onComplete();
            }

            @Override
            public void request(long n) {
                Subscriptions.requestIfNotNullOrAccumulate(secondUpstream, requested, n);
            }

            @Override
            public void cancel() {
                UniSubscription subscription = firstUpstream.getAndSet(EmptyUniSubscription.CANCELLED);
                if (subscription != null && subscription != EmptyUniSubscription.CANCELLED) {
                    subscription.cancel();
                }
                Subscriptions.cancel(secondUpstream);
            }

            @Override
            public Context context() {
                if (downstream instanceof ContextSupport) {
                    return ((ContextSupport) downstream).context();
                } else {
                    return Context.empty();
                }
            }

            /**
             * Called when we get the subscription from the upstream UNI
             *
             * @param subscription the subscription allowing to cancel the computation.
             */
            @Override
            public void onSubscribe(UniSubscription subscription) {
                if (firstUpstream.compareAndSet(null, subscription)) {
                    downstream.onSubscribe(this);
                }
            }

            /**
             * Called after we produced the {@link Flow.Publisher} and subscribe on it.
             *
             * @param subscription the subscription from the produced {@link Flow.Publisher}
             */
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                if (secondUpstream.compareAndSet(null, subscription)) {
                    long r = requested.getAndSet(0L);
                    if (r != 0L) {
                        subscription.request(r);
                    }
                }
            }

            @Override
            public void onItem(I item) {
                Multi<? extends O> publisher;

                try {
                    publisher = dataExtractor.apply(item);
                    if (publisher == null) {
                        throw new NullPointerException(MAPPER_RETURNED_NULL);
                    }
                    if (headersExtractor != null) {
                        headers.set(headersExtractor.apply(item));
                    }
                    if (statusExtractor != null) {
                        status.set(statusExtractor.apply(item));
                    }
                } catch (Throwable ex) {
                    downstream.onError(ex);
                    return;
                }

                publisher.subscribe(this);
            }

            @Override
            public void onFailure(Throwable failure) {
                downstream.onError(failure);
            }
        }

    }

}
