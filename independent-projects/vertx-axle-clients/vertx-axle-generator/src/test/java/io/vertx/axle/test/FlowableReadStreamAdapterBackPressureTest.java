package io.vertx.axle.test;

import io.reactivex.Flowable;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import io.vertx.lang.axle.test.TestSubscriber;
import io.vertx.lang.axle.test.ReadStreamAdapterBackPressureTest;
import io.vertx.axle.PublisherHelper;
import io.vertx.axle.FlowableReadStream;
import io.vertx.test.fakestream.FakeStream;
import org.junit.Test;
import org.reactivestreams.Publisher;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FlowableReadStreamAdapterBackPressureTest extends ReadStreamAdapterBackPressureTest<Publisher<Buffer>> {

  @Override
  protected long defaultMaxBufferSize() {
    return FlowableReadStream.DEFAULT_MAX_BUFFER_SIZE;
  }

  @Override
  protected Publisher<Buffer> toObservable(ReadStream<Buffer> stream, int maxBufferSize) {
    return PublisherHelper.toFlowable(stream, maxBufferSize);
  }

  @Override
  protected Publisher<Buffer> toObservable(ReadStream<Buffer> stream) {
    return PublisherHelper.toPublisher(stream);
  }

  @Override
  protected Publisher<Buffer> flatMap(Publisher<Buffer> obs, Function<Buffer, Publisher<Buffer>> f) {
    return Flowable.fromPublisher(obs).flatMap(f::apply);
  }

  @Override
  protected void subscribe(Publisher<Buffer> obs, TestSubscriber<Buffer> sub) {
    TestUtils.subscribe(obs, sub);
  }

  @Override
  protected Flowable<Buffer> concat(Publisher<Buffer> obs1, Publisher<Buffer> obs2) {
    return Flowable.concat(obs1, obs2);
  }

  @Test
  public void testSubscribeTwice() {
    FakeStream<Buffer> stream = new FakeStream<>();
    Publisher<Buffer> observable = toObservable(stream);
    TestSubscriber<Buffer> subscriber1 = new TestSubscriber<Buffer>().prefetch(0);
    TestSubscriber<Buffer> subscriber2 = new TestSubscriber<Buffer>().prefetch(0);
    subscribe(observable, subscriber1);
    subscribe(observable, subscriber2);
    subscriber2.assertError(err -> {
      assertTrue(err instanceof IllegalStateException);
    });
    subscriber2.assertEmpty();
  }

  @Test
  public void testHandletIsSetInDoOnSubscribe() {
    AtomicBoolean handlerSet = new AtomicBoolean();
    FakeStream<Buffer> stream = new FakeStream<Buffer>() {
      @Override
      public FakeStream<Buffer> handler(Handler<Buffer> handler) {
        handlerSet.set(true);
        return super.handler(handler);
      }
    };
    Flowable<Buffer> observable = Flowable.fromPublisher(toObservable(stream)).doOnSubscribe(disposable -> {
      assertTrue(handlerSet.get());
    });
    TestSubscriber<Buffer> subscriber = new TestSubscriber<>();
    subscribe(observable, subscriber);
    subscriber.assertEmpty();
  }
}
