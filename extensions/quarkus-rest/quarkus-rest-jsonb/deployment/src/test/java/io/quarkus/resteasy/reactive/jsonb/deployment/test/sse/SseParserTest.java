package io.quarkus.resteasy.reactive.jsonb.deployment.test.sse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.sse.InboundSseEvent;
import javax.ws.rs.sse.SseEvent;

import org.jboss.resteasy.reactive.client.QuarkusRestInboundSseEvent;
import org.jboss.resteasy.reactive.client.QuarkusRestSseEventSource;
import org.jboss.resteasy.reactive.client.SseParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.vertx.core.buffer.Buffer;

public class SseParserTest {

    @Test
    public void testParser() {
        // various EOF combinations
        testParser("data:foo\r\n\r\n", "foo", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:foo\n\n", "foo", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:foo\r\r", "foo", null, null, null, SseEvent.RECONNECT_NOT_SET);
        // split data line, with various EOF combinations
        testParser("data:foo\ndata:bar\n\n", "foo\nbar", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:foo\r\ndata:bar\r\n\r\n", "foo\nbar", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:foo\rdata:bar\r\r", "foo\nbar", null, null, null, SseEvent.RECONNECT_NOT_SET);
        // split data line with empty data in the middle
        testParser("data:foo\ndata\ndata:bar\n\n", "foo\n\nbar", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:foo\ndata:\ndata:bar\n\n", "foo\n\nbar", null, null, null, SseEvent.RECONNECT_NOT_SET);

        // no data: no event
        testParser("\n", null, null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:\n\n", null, null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data\n\n", null, null, null, null, SseEvent.RECONNECT_NOT_SET);

        // all fields
        testParser("data:DATA\nid:ID\n:COMMENT\nretry:23\nevent:NAME\n\n", "DATA", "COMMENT", "ID", "NAME", 23);
        // all fields and no data: no event
        testParser("id:ID\n:COMMENT\nretry:23\nevent:NAME\n\n", null, null, null, null, SseEvent.RECONNECT_NOT_SET);

        // optional space after colon
        testParser("data:foo\n\n", "foo", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data: foo\n\n", "foo", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:  foo\n\n", " foo", null, null, null, SseEvent.RECONNECT_NOT_SET);

        // UTF-8
        testParser("data: a¢€𐍈\n\n", "a¢€𐍈", null, null, null, SseEvent.RECONNECT_NOT_SET);

        // invalid retry is ignored
        testParser("data:DATA\nretry:-23\n\n", "DATA", null, null, null, SseEvent.RECONNECT_NOT_SET);
        testParser("data:DATA\nretry:ASD\n\n", "DATA", null, null, null, SseEvent.RECONNECT_NOT_SET);

        // two events
        testParser(Arrays.asList("data:foo\n\ndata:bar\n\n"),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("foo"),
                        new QuarkusRestInboundSseEvent(null, null)
                                .setData("bar")));
        // two events with data
        testParser(Arrays.asList("data:DATA\nid:ID\n:COMMENT\nretry:23\nevent:NAME\n\n"
                + "data:DATA2\nid:ID2\n:COMMENT2\nretry:232\nevent:NAME2\n\n"),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("DATA")
                        .setId("ID")
                        .setComment("COMMENT")
                        .setReconnectDelay(23)
                        .setName("NAME"),
                        new QuarkusRestInboundSseEvent(null, null)
                                .setData("DATA2")
                                .setId("ID2")
                                .setComment("COMMENT2")
                                .setReconnectDelay(232)
                                .setName("NAME2")));
        // two events with data, only ID is persistent
        testParser(Arrays.asList("data:DATA\nid:ID\n:COMMENT\nretry:23\nevent:NAME\n\n"
                + "data:DATA2\n\n"),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("DATA")
                        .setId("ID")
                        .setComment("COMMENT")
                        .setReconnectDelay(23)
                        .setName("NAME"),
                        new QuarkusRestInboundSseEvent(null, null)
                                .setData("DATA2")
                                .setId("ID")));

        // two events in two buffers
        testParser(Arrays.asList("data:foo\n\n", "data:bar\n\n"),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("foo"),
                        new QuarkusRestInboundSseEvent(null, null)
                                .setData("bar")));
        // two events in two buffers at awkward places
        testParser(Arrays.asList("data:foo\n\ndata:b", "ar\n\n"),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("foo"),
                        new QuarkusRestInboundSseEvent(null, null)
                                .setData("bar")));
        // one event in two buffers
        testParser(Arrays.asList("data:f", "oo\n\n"),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("foo")));
        // one event in two buffers within a utf-8 char
        testParserWithBytes(
                Arrays.asList(new byte[] { 'd', 'a', 't', 'a', ':', (byte) 0b11000010 },
                        new byte[] { (byte) 0b10100010, '\n', '\n' }),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("¢")));

        // BOM
        testParserWithBytes(
                Arrays.asList(new byte[] { (byte) 0xFE, (byte) 0xFF, 'd', 'a', 't', 'a', ':', 'f', 'o', 'o', '\n', '\n' }),
                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                        .setData("foo")));

        // invalid BOM location
        Assertions
                .assertThrows(IllegalStateException.class,
                        () -> testParserWithBytes(
                                Arrays.asList(new byte[] { 'd', 'a', 't', 'a', ':', 'f', 'o', 'o', '\n', '\n' },
                                        new byte[] { (byte) 0xFE, (byte) 0xFF, 'd', 'a', 't', 'a', ':', 'f', 'o', 'o', '\n',
                                                '\n' }),
                                Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                                        .setData("foo"))));
        // invalid UTF-8
        Assertions.assertThrows(IllegalStateException.class,
                () -> testParserWithBytes(
                        Arrays.asList(new byte[] { 'd', 'a', 't', 'a', ':', (byte) 0b1111_1000, 'f', 'o', 'o', '\n', '\n' }),
                        Arrays.asList()));
    }

    private void testParser(String event, String data, String comment, String lastId, String name, long reconnectDelay) {
        if (data != null) {
            testParser(Arrays.asList(event), Arrays.asList(new QuarkusRestInboundSseEvent(null, null)
                    .setData(data)
                    .setComment(comment)
                    .setId(lastId)
                    .setName(name)
                    .setReconnectDelay(reconnectDelay)));
        } else {
            testParser(Arrays.asList(event), Collections.emptyList());
        }
    }

    private void testParser(List<String> events, List<InboundSseEvent> expectedEvents) {
        testParserWithBytes(events.stream().map(str -> str.getBytes(StandardCharsets.UTF_8)).collect(Collectors.toList()),
                expectedEvents);
    }

    private void testParserWithBytes(List<byte[]> events, List<InboundSseEvent> expectedEvents) {
        QuarkusRestSseEventSource eventSource = new QuarkusRestSseEventSource(null, 500, TimeUnit.MILLISECONDS);
        SseParser parser = eventSource.getSseParser();
        CountDownLatch latch = new CountDownLatch(expectedEvents.size());
        List<InboundSseEvent> receivedEvents = new ArrayList<>(expectedEvents.size());
        eventSource.register(evt -> {
            latch.countDown();
            receivedEvents.add(evt);
        });
        for (byte[] event : events) {
            parser.handle(Buffer.buffer(event));
        }
        // this is really synchronous, so we can't timeout
        try {
            latch.await(1, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertEquals(expectedEvents, receivedEvents);
    }
}
