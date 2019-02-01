package io.vertx.lang.axle.test;

import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.test.core.VertxTestBase;
import io.vertx.test.fakestream.FakeStream;
import org.junit.Test;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public abstract class ReadStreamAdapterTestBase<B, O> extends VertxTestBase {

  protected abstract O toObservable(ReadStream<Buffer> stream);
  protected abstract B buffer(String s);
  protected abstract String string(B buffer);
  protected abstract void subscribe(O obs, TestSubscriber<B> sub);
  protected abstract O concat(O obs1, O obs2);

  @Test
  public void testReact() {
    FakeStream<Buffer> stream = new FakeStream<>();
    O observable = toObservable(stream);
    TestSubscriber<B> subscriber = new TestSubscriber<B>() {
      @Override
      protected void assertEquals(Object expected, Object actual) {
        super.assertEquals(string((B) expected), string((B) actual));
      }
    };
    subscribe(observable, subscriber);
    assertNotNull(stream.handler());
    assertNotNull(stream.endHandler());
    assertNotNull(stream.exceptionHandler());
    stream.emit(Buffer.buffer("foo"));
    subscriber.assertItem(buffer("foo")).assertEmpty();
    stream.emit(Buffer.buffer("bar"));
    subscriber.assertItem(buffer("bar")).assertEmpty();
    stream.end();
    subscriber.assertCompleted().assertEmpty();
    assertTrue(subscriber.isUnsubscribed());
    testComplete();
  }

  @Test
  public void testConcat() {
    FakeStream<Buffer> stream1 = new FakeStream<>();
    FakeStream<Buffer> stream2 = new FakeStream<>();
    O observable1 = toObservable(stream1);
    O observable2 = toObservable(stream2);
    O observable = concat(observable1, observable2);
    TestSubscriber<B> observer = new TestSubscriber<B>() {
      @Override
      public void onNext(B next) {
        switch (string(next)) {
          case "item1":
            assertNotNull(stream1.handler());
            assertNull(stream2.handler());
            stream1.end();
            break;
          case "item2":
            assertNull(stream1.handler());
            assertNotNull(stream2.handler());
            stream2.end();
            break;
          default:
            fail();
        }
      }
      @Override
      public void onError(Throwable e) {
        super.onError(e);
        fail();
      }
      @Override
      public void onCompleted() {
        super.onCompleted();
        testComplete();
      }
    };
    subscribe(observable, observer);
    stream1.emit(Buffer.buffer("item1"));
    assertNull(stream1.handler());
    stream2.emit(Buffer.buffer("item2"));
    assertTrue(observer.isUnsubscribed());
    assertNull(stream2.handler());
    await();
  }

  @Test
  public void testDataHandlerShouldBeSetAndUnsetAfterOtherHandlers() {
    FakeStream<Buffer> stream = new FakeStream<Buffer>() {
      @Override
      public FakeStream<Buffer> handler(Handler<Buffer> handler) {
        if (handler == null) {
          assertNull(exceptionHandler());
          assertNull(endHandler());
        } else {
          assertNotNull(exceptionHandler());
          assertNotNull(endHandler());
        }
        return super.handler(handler);
      }
    };
    O observable = toObservable(stream);
    TestSubscriber<B> subscriber = new TestSubscriber<>();
    subscribe(observable, subscriber);
    subscriber.unsubscribe();
  }

  @Test
  public void testOnSubscribeHandlerIsSetLast() {
    FakeStream<Buffer> stream = new FakeStream<Buffer>() {
      @Override
      public FakeStream<Buffer> handler(Handler<Buffer> handler) {
        assertNotNull(exceptionHandler());
        assertNotNull(endHandler());
        return super.handler(handler);
      }
    };
    O observable = toObservable(stream);
    TestSubscriber<B> subscriber = new TestSubscriber<>();
    subscribe(observable, subscriber);
  }

  @Test
  public void testHandlers() {
    FakeStream<Buffer> stream = new FakeStream<>();
    O observable = toObservable(stream);
    TestSubscriber<B> subscriber = new TestSubscriber<>();
    subscribe(observable, subscriber);
    assertNotNull(stream.handler());
    assertNotNull(stream.endHandler());
    assertNotNull(stream.exceptionHandler());
    subscriber.unsubscribe();
    assertNull(stream.handler());
    assertNull(stream.endHandler());
    assertNull(stream.exceptionHandler());
  }
}
