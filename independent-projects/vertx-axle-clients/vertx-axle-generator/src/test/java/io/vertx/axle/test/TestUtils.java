package io.vertx.axle.test;

import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.lang.axle.test.TestSubscriber;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class TestUtils {

  public static <T>  void subscribe(Publisher<T> obs, TestSubscriber<T> sub) {
    obs.subscribe(new Subscriber<T>() {
      boolean unsubscribed;
      @Override
      public void onSubscribe(Subscription s) {
        sub.onSubscribe(new TestSubscriber.Subscription() {
          @Override
          public void fetch(long val) {
            if (val > 0) {
              s.request(val);
            }
          }
          @Override
          public void unsubscribe() {
            unsubscribed = true;
            s.cancel();
          }
          @Override
          public boolean isUnsubscribed() {
            return unsubscribed;
          }
        });

      }
      @Override
      public void onNext(T buffer) {
        sub.onNext(buffer);
      }
      @Override
      public void onError(Throwable t) {
        unsubscribed = true;
        sub.onError(t);
      }
      @Override
      public void onComplete() {
        unsubscribed = true;
        sub.onCompleted();
      }
    });
  }

  public static <T> void subscribe(Observable<T> obs, TestSubscriber<T> sub) {
    obs.subscribe(sub::onNext,
      sub::onError,
      sub::onCompleted,
      disposable -> {
        sub.onSubscribe(new TestSubscriber.Subscription() {
          @Override
          public void fetch(long val) {}
          @Override
          public void unsubscribe() {
            disposable.dispose();
          }
          @Override
          public boolean isUnsubscribed() {
            return disposable.isDisposed();
          }
        });
      });
  }

  public static <T> void subscribe(Single<T> obs, TestSubscriber<T> sub) {
    obs.subscribe(sub::onNext, sub::onError);
  }

  public static <T> void subscribe(Maybe<T> obs, TestSubscriber<T> sub) {
    obs.subscribe(sub::onNext, sub::onError, sub::onCompleted);
  }
}
