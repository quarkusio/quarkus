package io.vertx.axle;

import io.reactivex.Flowable;
import io.reactivex.FlowableSubscriber;
import io.reactivex.internal.subscriptions.BasicIntQueueSubscription;
import io.reactivex.internal.subscriptions.EmptySubscription;
import io.reactivex.processors.UnicastProcessor;
import io.vertx.core.streams.ReadStream;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class FlowableReadStream<T, U> extends Flowable<U> {

  public static final long DEFAULT_MAX_BUFFER_SIZE = 256;

  private final ReadStream<T> stream;
  private final Function<T, U> f;
  private final AtomicReference<Subscription> current;

  public FlowableReadStream(ReadStream<T> stream, long maxBufferSize, Function<T, U> f) {

    stream.pause();

    this.stream = stream;
    this.f = f;
    this.current = new AtomicReference<>();
  }

  private void release() {
    Subscription sub = current.get();
    if (sub != null) {
      if (current.compareAndSet(sub, null)) {
        try {
          stream.exceptionHandler(null);
          stream.endHandler(null);
          stream.handler(null);
        } catch (Exception ignore) {
        } finally {
          stream.resume();
        }
      }
    }
  }

  @Override
  protected void subscribeActual(Subscriber<? super U> subscriber) {

    Subscription sub = new Subscription() {
      @Override
      public void request(long l) {
        if (current.get() == this) {
          stream.fetch(l);
        }
      }

      @Override
      public void cancel() {
        release();
      }
    };
    if (!current.compareAndSet(null, sub)) {
      EmptySubscription.error(new IllegalStateException("This processor allows only a single Subscriber"), subscriber);
      return;
    }

    stream.pause();

    stream.endHandler(v -> {
      release();
      subscriber.onComplete();
    });
    stream.exceptionHandler(err -> {
      release();
      subscriber.onError(err);
    });
    stream.handler(item -> {
      subscriber.onNext(f.apply(item));
    });

    subscriber.onSubscribe(sub);
  }
}
