package io.vertx.axle;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.disposables.Disposable;
import io.reactivex.plugins.RxJavaPlugins;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class AsyncResultCompletable extends Completable {

  private final Consumer<Handler<AsyncResult<Void>>> subscriptionConsumer;

  public static Completable toCompletable(Consumer<Handler<AsyncResult<Void>>> subscriptionConsumer) {
    return RxJavaPlugins.onAssembly(new AsyncResultCompletable(subscriptionConsumer));
  }

  public AsyncResultCompletable(Consumer<Handler<AsyncResult<Void>>> subscriptionConsumer) {
    this.subscriptionConsumer = subscriptionConsumer;
  }

  @Override
  protected void subscribeActual(CompletableObserver observer) {
    AtomicBoolean disposed = new AtomicBoolean();
    observer.onSubscribe(new Disposable() {
      @Override
      public void dispose() {
        disposed.set(true);
      }
      @Override
      public boolean isDisposed() {
        return disposed.get();
      }
    });
    if (!disposed.get()) {
      try {
        subscriptionConsumer.accept(ar -> {
          if (!disposed.getAndSet(true)) {
            if (ar.succeeded()) {
              try {
                observer.onComplete();
              } catch (Throwable ignore) {
              }
            } else {
              try {
                observer.onError(ar.cause());
              } catch (Throwable ignore) {
              }
            }
          }
        });
      } catch (Exception e) {
        if (!disposed.getAndSet(true)) {
          try {
            observer.onError(e);
          } catch (Throwable ignore) {
          }
        }
      }
    }
  }
}
