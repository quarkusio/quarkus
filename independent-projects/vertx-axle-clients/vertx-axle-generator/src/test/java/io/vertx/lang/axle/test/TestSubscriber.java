package io.vertx.lang.axle.test;

import org.junit.Assert;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestSubscriber<T> {

  private static final Object completed = new Object() {
    @Override
    public String toString() {
      return "Completed";
    }
  };

  private long prefetch = Long.MAX_VALUE;
  private final ArrayBlockingQueue<Object> events = new ArrayBlockingQueue<>(100);
  private Subscription subscription;
  private long requested;

  public void onSubscribe(Subscription sub) {
    subscription = sub;
    request(prefetch);
  }

  public interface Subscription {

    void fetch(long val);

    void unsubscribe();

    boolean isUnsubscribed();

  }

  public TestSubscriber<T> prefetch(long value) {
    prefetch = value;
    return this;
  }

  public TestSubscriber<T> unsubscribe() {
    subscription.unsubscribe();
    return this;
  }

  public boolean isSubscribed() {
    return !isUnsubscribed();
  }

  public boolean isUnsubscribed() {
    return subscription.isUnsubscribed();
  }

  public TestSubscriber<T> request(long val) {
    if (val < 0) {
      throw new IllegalArgumentException();
    }
    requested += val;
    if (requested < 0L) {
      requested = Long.MAX_VALUE;
    }
    subscription.fetch(val);
    return this;
  }

  public void onCompleted() { events.add(completed); }

  public void onError(Throwable e) {
    events.add(e);
  }

  public void onNext(T t) {
    if (requested < Long.MAX_VALUE) {
      if (requested < 1) {
        throw new IllegalStateException("Cannot handle non requested item");
      }
    }
    events.add(t);
  }

  public TestSubscriber<T> assertItem(T expected) {
    return assertEvent(expected);
  }

  public TestSubscriber<T> assertItems(T... expected) {
    for (T item : expected) {
      assertItem(item);
    }
    return this;
  }

  public TestSubscriber<T> assertError(Throwable expected) {
    return assertEvent(expected);
  }

  public TestSubscriber<T> assertError(Consumer<Throwable> checker) {
    return assertEvent((obj) -> {
      if (obj instanceof Throwable) {
        checker.accept((Throwable) obj);
      } else {
        Assert.fail("Was expecting a throwable");
      }
    });
  }

  public TestSubscriber<T> assertCompleted() {
    return assertEvent(completed);
  }

  public TestSubscriber<T> assertEmpty() {
    if (!events.isEmpty()) {
      throw new AssertionError("Was expecting no events instead of " + events);
    }
    return this;
  }

  private TestSubscriber<T> assertEvent(Object expected) {
    return assertEvent(event -> {
      if (expected == completed) {
        Assert.assertEquals(completed, event);
      } else if (expected instanceof Throwable) {
        Assert.assertEquals(expected, event);
      } else {
        assertEquals(expected, event);
      }
    });
  }

  private TestSubscriber<T> assertEvent(Consumer<Object> checker) {
    Object event;
    try {
      event = events.poll(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
    if (event == null) {
      throw new AssertionError("Was expecting at least one event");
    }
    checker.accept(event);
    return this;
  }

  protected void assertEquals(java.lang.Object expected, java.lang.Object actual) {
    Assert.assertEquals(expected, actual);
  }
}
