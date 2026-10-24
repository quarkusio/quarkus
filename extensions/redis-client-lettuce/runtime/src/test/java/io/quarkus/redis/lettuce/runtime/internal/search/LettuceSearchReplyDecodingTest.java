package io.quarkus.redis.lettuce.runtime.internal.search;

import static io.quarkus.redis.lettuce.runtime.internal.search.LettuceReactiveSearchCommandsImpl.decodeAggregateResponse;
import static io.quarkus.redis.lettuce.runtime.internal.search.LettuceReactiveSearchCommandsImpl.decodeSearchResponse;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.search.AggregationResponse;
import io.quarkus.redis.datasource.search.Document;
import io.quarkus.redis.datasource.search.SearchQueryResponse;
import io.quarkus.redis.lettuce.runtime.internal.search.LettuceReactiveSearchCommandsImpl.ResultLayout;
import io.vertx.core.buffer.Buffer;
import io.vertx.redis.client.Response;
import io.vertx.redis.client.impl.types.BulkType;
import io.vertx.redis.client.impl.types.MultiType;
import io.vertx.redis.client.impl.types.NumberType;

class LettuceSearchReplyDecodingTest {

    private static final ResultLayout EVERYTHING = new ResultLayout(true, true, true, true);

    @Test
    void resp2SearchReplyWithEveryOptionalPart() {
        // WITHSCORES WITHPAYLOADS WITHSORTKEYS: id, score, payload, sort key, fields
        Response reply = multi(number(2),
                bulk("doc:1"), bulk("1.5"), bulk("p1"), bulk("$heat"), multi(bulk("title"), bulk("Heat")),
                bulk("doc:2"), bulk("0.5"), null, null, multi(bulk("title"), bulk("Alien")));

        SearchQueryResponse response = decodeSearchResponse(reply, EVERYTHING);

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.documents()).hasSize(2);
        Document first = response.documents().get(0);
        assertThat(first.key()).isEqualTo("doc:1");
        assertThat(first.score()).isEqualTo(1.5);
        assertThat(first.payload().toString()).isEqualTo("p1");
        assertThat(first.property("title").asString()).isEqualTo("Heat");
        Document second = response.documents().get(1);
        assertThat(second.key()).isEqualTo("doc:2");
        assertThat(second.score()).isEqualTo(0.5);
        assertThat(second.payload()).isNull();
        assertThat(second.property("title").asString()).isEqualTo("Alien");
    }

    @Test
    void resp2SearchReplyWithNoContent() {
        Response reply = multi(number(2), bulk("doc:1"), bulk("doc:2"));

        SearchQueryResponse response = decodeSearchResponse(reply, new ResultLayout(false, false, false, false));

        assertThat(response.count()).isEqualTo(2);
        assertThat(response.documents()).extracting(Document::key).containsExactly("doc:1", "doc:2");
        assertThat(response.documents()).allSatisfy(d -> {
            assertThat(d.score()).isEqualTo(1.0);
            assertThat(d.payload()).isNull();
            assertThat(d.properties()).isEmpty();
        });
    }

    @Test
    void resp2SearchReplyWithExplainScore() {
        // EXPLAINSCORE replaces the score with the score and its explanation
        Response reply = multi(number(1),
                bulk("doc:1"), multi(bulk("1.5"), multi(bulk("Final TFIDF : ..."))), multi(bulk("title"), bulk("Heat")));

        SearchQueryResponse response = decodeSearchResponse(reply, new ResultLayout(true, false, false, true));

        assertThat(response.documents()).singleElement().satisfies(d -> {
            assertThat(d.key()).isEqualTo("doc:1");
            assertThat(d.score()).isEqualTo(1.5);
            assertThat(d.property("title").asString()).isEqualTo("Heat");
        });
    }

    @Test
    void resp3SearchReply() {
        Response reply = map("attributes", multi(), "format", bulk("STRING"), "total_results", number(2),
                "results", multi(
                        map("id", bulk("doc:1"), "score", number(1.5), "payload", bulk("p1"),
                                "extra_attributes", map("title", bulk("Heat"))),
                        map("id", bulk("doc:2"), "score", multi(number(0.5), multi(bulk("Final TFIDF : ..."))),
                                "extra_attributes", map("title", bulk("Alien")))),
                "warning", multi());

        // The layout does not matter for RESP3, the parts are named
        SearchQueryResponse response = decodeSearchResponse(reply, ResultLayout.DEFAULT);

        assertThat(response.count()).isEqualTo(2);
        Document first = response.documents().get(0);
        assertThat(first.key()).isEqualTo("doc:1");
        assertThat(first.score()).isEqualTo(1.5);
        assertThat(first.payload().toString()).isEqualTo("p1");
        assertThat(first.property("title").asString()).isEqualTo("Heat");
        Document second = response.documents().get(1);
        assertThat(second.key()).isEqualTo("doc:2");
        assertThat(second.score()).isEqualTo(0.5);
        assertThat(second.payload()).isNull();
        assertThat(second.property("title").asString()).isEqualTo("Alien");
    }

    @Test
    void emptySearchReplies() {
        assertThat(decodeSearchResponse(null, EVERYTHING).count()).isEqualTo(0);
        assertThat(decodeSearchResponse(multi(number(0)), EVERYTHING).documents()).isEmpty();
        assertThat(decodeSearchResponse(map("total_results", number(0), "results", multi()), EVERYTHING).documents())
                .isEmpty();
    }

    @Test
    void resp2AggregateReplyWithCursor() {
        Response reply = multi(
                multi(number(1), multi(bulk("country"), bulk("fr"), bulk("count"), bulk("3"))),
                number(42));

        AggregationResponse response = decodeAggregateResponse(reply, true);

        assertThat(response.cursor()).isEqualTo(42);
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.documents().get(0).property("country").asString()).isEqualTo("fr");
        assertThat(response.documents().get(0).property("count").asInteger()).isEqualTo(3);
    }

    @Test
    void resp3AggregateReplyWithoutCursor() {
        Response reply = map("attributes", multi(), "format", bulk("STRING"), "total_results", number(1),
                "results", multi(map("extra_attributes", map("country", bulk("fr"), "count", number(3)))),
                "warning", multi());

        AggregationResponse response = decodeAggregateResponse(reply, false);

        assertThat(response.cursor()).isEqualTo(-1);
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.documents().get(0).property("country").asString()).isEqualTo("fr");
        assertThat(response.documents().get(0).property("count").asInteger()).isEqualTo(3);
    }

    @Test
    void resp3SearchReplyFlattenedInsideATransaction() {
        Response reply = flattened("attributes", multi(), "format", bulk("STRING"), "total_results", number(1),
                "results", multi(flattened("id", bulk("doc:1"), "score", number(1.5), "payload", bulk("p1"),
                        "extra_attributes", flattened("title", bulk("Heat"), "rating", number(8)))),
                "warning", multi());

        SearchQueryResponse response = decodeSearchResponse(reply, EVERYTHING);

        assertThat(response.count()).isEqualTo(1);
        assertThat(response.documents()).singleElement().satisfies(d -> {
            assertThat(d.key()).isEqualTo("doc:1");
            assertThat(d.score()).isEqualTo(1.5);
            assertThat(d.payload().toString()).isEqualTo("p1");
            assertThat(d.property("title").asString()).isEqualTo("Heat");
            assertThat(d.property("rating").asInteger()).isEqualTo(8);
        });
    }

    @Test
    void resp3AggregateReplyFlattenedInsideATransaction() {
        Response reply = multi(
                flattened("attributes", multi(), "format", bulk("STRING"), "total_results", number(1),
                        "results", multi(flattened("extra_attributes", flattened("country", bulk("fr"), "count", number(3)))),
                        "warning", multi()),
                number(0));

        AggregationResponse response = decodeAggregateResponse(reply, true);

        assertThat(response.cursor()).isEqualTo(0);
        assertThat(response.count()).isEqualTo(1);
        assertThat(response.documents().get(0).property("country").asString()).isEqualTo("fr");
        assertThat(response.documents().get(0).property("count").asInteger()).isEqualTo(3);
    }

    private static Response bulk(String value) {
        return BulkType.create(Buffer.buffer(value), false);
    }

    private static Response number(Number value) {
        return NumberType.create(value);
    }

    private static Response multi(Response... elements) {
        MultiType multi = MultiType.create(elements.length, false);
        for (Response element : elements) {
            multi.add(element);
        }
        return multi;
    }

    /**
     * A RESP3 map as Lettuce 8.0.0.BETA1 delivers it to a command queued in {@code MULTI}/{@code EXEC}: a plain array
     * of alternating keys and values (redis/lettuce#3937).
     */
    private static Response flattened(Object... keysAndValues) {
        MultiType multi = MultiType.create(keysAndValues.length, false);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            multi.add(bulk((String) keysAndValues[i]));
            multi.add((Response) keysAndValues[i + 1]);
        }
        return multi;
    }

    /** A map multi from alternating {@code String} keys and {@link Response} values. */
    private static Response map(Object... keysAndValues) {
        MultiType multi = MultiType.create(keysAndValues.length / 2, true);
        for (int i = 0; i < keysAndValues.length; i += 2) {
            multi.add(bulk((String) keysAndValues[i]));
            multi.add((Response) keysAndValues[i + 1]);
        }
        return multi;
    }

}
