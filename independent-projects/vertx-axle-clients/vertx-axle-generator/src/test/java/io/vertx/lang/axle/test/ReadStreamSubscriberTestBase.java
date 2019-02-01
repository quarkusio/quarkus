package io.vertx.lang.axle.test;

import io.vertx.core.streams.ReadStream;
import io.vertx.test.core.VertxTestBase;
import org.junit.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class ReadStreamSubscriberTestBase extends VertxTestBase {

  public abstract long bufferSize();

  private final long BUFFER_SIZE = bufferSize();

  public abstract class Sender {

    protected ReadStream<String> stream;
    protected long requested;
    protected int seq;

    protected  abstract void emit();

    void emit(int times) {
      for (int i = 0;i < times;i++) {
        emit();
      }
    }

    protected abstract void complete();

    protected abstract void fail(Throwable cause);

    void assertRequested(long expected) {
      assertEquals(expected, requested);
    }

    long available() {
      return requested - seq;
    }
  }

  private class Receiver extends ArrayDeque<Object> {

    final Object DONE = new Object();

    void handle(String item) {
      add(item);
    }

    void handleException(Throwable t) {
      add(t);
    }

    void handleEnd(Void v) {
      add(DONE);
    }

    void subscribe(ReadStream<String> sender) {
      sender.exceptionHandler(this::handleException);
      sender.endHandler(this::handleEnd);
      sender.handler(this::handle);
    }

    Receiver assertEmpty() {
      assertEquals(Collections.emptyList(), new ArrayList<>(this));
      return this;
    }

    Receiver assertItems(String... items) {
      ArrayList<Object> actual = new ArrayList<>();
      while (size() > 0 && actual.size() < items.length) {
        actual.add(remove());
      }
      assertEquals(Arrays.asList(items), actual);
      return this;
    }

    void assertEnded() {
      assertEquals(DONE, remove());
      assertEmpty();
    }
  }

  protected abstract Sender sender();


  @Test
  public void testInitial() throws Exception {
    Sender sender = sender();
    sender.assertRequested(0);
    Receiver receiver = new Receiver();
    receiver.subscribe(sender.stream);
    sender.assertRequested(BUFFER_SIZE);
    while (sender.seq < BUFFER_SIZE / 2) {
      sender.emit();
      sender.assertRequested(BUFFER_SIZE);
    }
    long i = BUFFER_SIZE - (sender.seq - 1);
    sender.emit();
    sender.assertRequested(BUFFER_SIZE + i);
  }

  @Test
  public void testPause() {
    Sender sender = sender();
    sender.stream.resume();
    sender.stream.pause();
    Receiver receiver = new Receiver();
    receiver.subscribe(sender.stream);
    for (int i = 0; i < BUFFER_SIZE; i++) {
      sender.emit();
      assertEquals(BUFFER_SIZE, sender.requested);
    }
    assertEquals(0, sender.available());
    receiver.assertEmpty();
    sender.stream.resume();
    assertEquals(BUFFER_SIZE, sender.available());
    receiver.assertItems("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15");
    receiver.assertEmpty();
  }

  @Test
  public void testCompletion() {
    Sender sender = sender();
    Receiver receiver = new Receiver();
    receiver.subscribe(sender.stream);
    sender.complete();
    receiver.assertEnded();
  }

  @Test
  public void testCompletionWhenPaused() {
    Sender sender = sender();
    sender.stream.pause();
    Receiver receiver = new Receiver();
    receiver.subscribe(sender.stream);
    sender.emit(3);
    sender.complete();
    sender.stream.resume();
    receiver.assertItems("0", "1", "2").assertEnded();
  }

  @Test
  public void testSetNullHandlersInEndHandler() {
    Sender sender = sender();
    AtomicInteger count = new AtomicInteger();
    sender.stream.endHandler(v -> {
      count.incrementAndGet();
      sender.stream.handler(null);
      sender.stream.endHandler(null);
      sender.stream.exceptionHandler(null);
    });
    sender.stream.handler(item -> {});
    sender.complete();
    assertEquals(1, count.get());
  }

  @Test
  public void testSetHandlersAfterCompletion() {
    Sender sender = sender();
    sender.stream.handler(item -> {});
    sender.complete();
    try {
      sender.stream.endHandler(v -> {});
      fail();
    } catch (IllegalStateException expected) {
    }
    sender.stream.endHandler(null);
    try {
      sender.stream.exceptionHandler(v -> {});
      fail();
    } catch (IllegalStateException expected) {
    }
    sender.stream.exceptionHandler(null);
  }

  @Test
  public void testSetHandlersAfterError() {
    Sender sender = sender();
    sender.stream.handler(item -> {});
    sender.fail(new Throwable());
    try {
      sender.stream.endHandler(v -> {});
      fail();
    } catch (IllegalStateException expected) {
    }
    sender.stream.endHandler(null);
    try {
      sender.stream.exceptionHandler(v -> {});
      fail();
    } catch (IllegalStateException expected) {
    }
    sender.stream.exceptionHandler(null);
  }

  @Test
  public void testDontDeliverCompletionWhenPausedWithPendingBuffers() {
    Sender sender = sender();
    AtomicInteger failed = new AtomicInteger();
    AtomicInteger completed = new AtomicInteger();
    sender.stream.endHandler(v -> completed.incrementAndGet());
    sender.stream.exceptionHandler(v -> failed.incrementAndGet());
    sender.stream.handler(item -> {});
    sender.stream.pause();
    sender.emit();
    sender.complete();
    assertEquals(0, completed.get());
    sender.stream.resume();
    assertEquals(1, completed.get());
    assertEquals(0, failed.get());
  }

  @Test
  public void testDontDeliverErrorWhenPausedWithPendingBuffers() {
    Sender sender = sender();
    AtomicInteger failed = new AtomicInteger();
    AtomicInteger completed = new AtomicInteger();
    sender.stream.endHandler(v -> completed.incrementAndGet());
    sender.stream.exceptionHandler(v -> failed.incrementAndGet());
    sender.stream.handler(item -> {});
    sender.stream.pause();
    sender.emit();
    RuntimeException cause = new RuntimeException();
    sender.fail(cause);
    assertEquals(0, completed.get());
    assertEquals(0, failed.get());
    sender.stream.resume();
    assertEquals(1, completed.get());
    assertEquals(1, failed.get());
  }

  @Test
  public void testSetHandlersAfterCompletionButPending() {
    Sender sender = sender();
    sender.stream.handler(item -> {});
    sender.stream.pause();
    sender.emit();
    sender.complete();
    sender.stream.exceptionHandler(err -> {});
    sender.stream.exceptionHandler(null);
    sender.stream.endHandler(v -> {});
    sender.stream.endHandler(null);
  }

  @Test
  public void testSetHandlersAfterErrorButPending() {
    Sender sender = sender();
    sender.stream.handler(item -> {});
    sender.stream.pause();
    sender.emit();
    sender.fail(new Throwable());
    sender.stream.exceptionHandler(err -> {});
    sender.stream.exceptionHandler(null);
    sender.stream.endHandler(v -> {});
    sender.stream.endHandler(null);
  }
}
