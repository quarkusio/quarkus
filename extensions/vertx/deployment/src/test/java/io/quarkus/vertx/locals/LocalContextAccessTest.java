package io.quarkus.vertx.locals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.common.vertx.VertxContext;
import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;

public class LocalContextAccessTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap
                    .create(JavaArchive.class).addClasses(BeanAccessingContext.class));

    @Inject
    Vertx vertx;

    @Inject
    BeanAccessingContext bean;

    @Test
    public void testGlobalAccessFromEventLoop() throws ExecutionException, InterruptedException, TimeoutException {
        Context context = vertx.getOrCreateContext();
        CompletableFuture<Throwable> get = new CompletableFuture<>();
        CompletableFuture<Throwable> put = new CompletableFuture<>();
        CompletableFuture<Throwable> remove = new CompletableFuture<>();

        context.runOnContext(x -> {
            try {
                bean.getGlobal();
                get.completeExceptionally(new Exception("Exception expected as using get is forbidden"));
            } catch (Exception e) {
                get.complete(e);
            }
        });
        Throwable t = get.toCompletableFuture().get(5, TimeUnit.SECONDS);
        Assertions.assertThat(t).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Context.get()");

        context.runOnContext(x -> {
            try {
                bean.putGlobal();
                put.completeExceptionally(new Exception("Exception expected as using put is forbidden"));
            } catch (Exception e) {
                put.complete(e);
            }
        });
        t = put.toCompletableFuture().get(5, TimeUnit.SECONDS);
        Assertions.assertThat(t).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Context.put()");

        context.runOnContext(x -> {
            try {
                bean.removeGlobal();
                remove.completeExceptionally(new Exception("Exception expected as using remove is forbidden"));
            } catch (Exception e) {
                remove.complete(e);
            }
        });
        t = remove.toCompletableFuture().get(5, TimeUnit.SECONDS);
        Assertions.assertThat(t).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Context.remove()");
    }

    @Test
    public void testLocalAccessFromEventLoop() throws ExecutionException, InterruptedException, TimeoutException {
        Context context = vertx.getOrCreateContext();
        CompletableFuture<Throwable> get = new CompletableFuture<>();
        CompletableFuture<Throwable> put = new CompletableFuture<>();
        CompletableFuture<Throwable> remove = new CompletableFuture<>();

        context.runOnContext(x -> {
            try {
                bean.getLocal();
                get.completeExceptionally(new Exception("Exception expected as using get is forbidden"));
            } catch (Exception e) {
                get.complete(e);
            }
        });
        Throwable t = get.toCompletableFuture().get(5, TimeUnit.SECONDS);
        Assertions.assertThat(t).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Context.getLocal()");

        context.runOnContext(x -> {
            try {
                bean.putLocal();
                put.completeExceptionally(new Exception("Exception expected as using put is forbidden"));
            } catch (Exception e) {
                put.complete(e);
            }
        });
        t = put.toCompletableFuture().get(5, TimeUnit.SECONDS);
        Assertions.assertThat(t).isInstanceOf(UnsupportedOperationException.class).hasMessageContaining("Context.putLocal()");

        context.runOnContext(x -> {
            try {
                bean.removeLocal();
                remove.completeExceptionally(new Exception("Exception expected as using remove is forbidden"));
            } catch (Exception e) {
                remove.complete(e);
            }
        });
        t = remove.toCompletableFuture().get(5, TimeUnit.SECONDS);
        Assertions.assertThat(t).isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Context.removeLocal()");
    }

    @Test
    public void testLocalAccessFromDuplicatedContext() throws ExecutionException, InterruptedException, TimeoutException {
        ContextInternal context = (ContextInternal) vertx.getOrCreateContext();
        CompletableFuture<Void> get = new CompletableFuture<>();
        CompletableFuture<Void> put = new CompletableFuture<>();
        CompletableFuture<Void> remove = new CompletableFuture<>();

        Context local = VertxContext.getOrCreateDuplicatedContext(context);

        local.runOnContext(x -> {
            try {
                bean.putLocal();
                put.complete(null);
            } catch (Exception e) {
                get.completeExceptionally(e);
            }
        });
        put.toCompletableFuture().get(5, TimeUnit.SECONDS);

        local.runOnContext(x -> {
            try {
                Assertions.assertThat(bean.getLocal()).isEqualTo("bar");
                get.complete(null);
            } catch (Exception e) {
                get.completeExceptionally(e);
            }
        });
        get.toCompletableFuture().get(5, TimeUnit.SECONDS);

        local.runOnContext(x -> {
            try {
                Assertions.assertThat(bean.removeLocal()).isTrue();
                remove.complete(null);
            } catch (Exception e) {
                remove.completeExceptionally(e);
            }
        });
        remove.toCompletableFuture().get(5, TimeUnit.SECONDS);
    }

    @ApplicationScoped
    public static class BeanAccessingContext {

        public String getGlobal() {
            return Vertx.currentContext().get("foo");
        }

        public void putGlobal() {
            Vertx.currentContext().put("foo", "bar");
        }

        public void removeGlobal() {
            Vertx.currentContext().remove("foo");
        }

        public String getLocal() {
            return Vertx.currentContext().getLocal("foo");
        }

        public void putLocal() {
            Vertx.currentContext().putLocal("foo", "bar");
        }

        public boolean removeLocal() {
            return Vertx.currentContext().removeLocal("foo");
        }

    }

}
