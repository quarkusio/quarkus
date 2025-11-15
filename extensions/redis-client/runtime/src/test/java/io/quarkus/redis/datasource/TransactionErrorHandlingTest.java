
package io.quarkus.redis.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.throwable;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.redis.datasource.transactions.OptimisticLockingTransactionResult;
import io.quarkus.redis.datasource.transactions.TransactionResult;
import io.quarkus.redis.runtime.datasource.BlockingRedisDataSourceImpl;
import io.quarkus.redis.runtime.datasource.ReactiveRedisDataSourceImpl;
import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.redis.client.impl.types.ErrorType;

@RequiresRedis6OrHigher
public class TransactionErrorHandlingTest extends DatasourceTestBase {
    private RedisDataSource blocking;
    private ReactiveRedisDataSource reactive;

    @BeforeEach
    void initialize() {
        blocking = new BlockingRedisDataSourceImpl(vertx, redis, api, Duration.ofSeconds(10));
        reactive = new ReactiveRedisDataSourceImpl(vertx, redis, api);
    }

    @AfterEach
    public void clear() {
        blocking.flushall();
    }

    // clientFailure: the Vert.x Redis client detects an error, the command is not sent to Redis at all
    // earlyFailure: the Redis server detects an error during command submission
    // lateFailure: the Redis server detects an error during `EXEC`
    // casFailure: the Redis server detects that a watched key was modified externally

    // ---

    @Test
    public void blockingTx_success() {
        blocking.value(String.class).set(key, "hello");

        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set(key, "foobar");
        });

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(1);
        assertThat((Object) result.get(0)).isNull();

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    @Test
    public void blockingTx_clientFailure() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(tx -> {
                tx.value(String.class).set(key, "foobar");
                tx.execute(Command.SET, key); // missing argument
            });
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTx_earlyFailure() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(tx -> {
                tx.value(String.class).set(key, "foobar");
                tx.execute("nonexisting_command");
            });
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTx_lateFailure() {
        blocking.value(String.class).set(key, "hello");

        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set(key, "foobar");
            tx.list(String.class).lpop(key);
        });

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1))
                .asInstanceOf(throwable(ErrorType.class))
                .hasMessageContaining("WRONGTYPE Operation against a key holding the wrong kind of value");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    @Test
    public void blockingTxWithWatch_success() {
        blocking.value(String.class).set(key, "hello");

        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set(key, "foobar");
        }, key);

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(1);
        assertThat((Object) result.get(0)).isNull();

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    @Test
    public void blockingTxWithWatch_clientFailure() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(tx -> {
                tx.value(String.class).set(key, "foobar");
                tx.execute(Command.SET, key); // missing argument
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithWatch_earlyFailure() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(tx -> {
                tx.value(String.class).set(key, "foobar");
                tx.execute("nonexisting_command");
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithWatch_lateFailure() {
        blocking.value(String.class).set(key, "hello");

        TransactionResult result = blocking.withTransaction(tx -> {
            tx.value(String.class).set(key, "foobar");
            tx.list(String.class).lpop(key);
        }, key);

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1))
                .asInstanceOf(throwable(ErrorType.class))
                .hasMessageContaining("WRONGTYPE Operation against a key holding the wrong kind of value");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    @Test
    public void blockingTxWithWatch_casFailure() {
        CompletableFuture<Void> begin = new CompletableFuture<>();
        CompletableFuture<Void> end = new CompletableFuture<>();

        blocking.value(String.class).set(key, "hello");

        new Thread(() -> {
            begin.join();
            blocking.value(String.class).set(key, "foobar");
            end.complete(null);
        }).start();

        TransactionResult result = blocking.withTransaction(tx -> {
            begin.complete(null);
            end.join();
            tx.value(String.class).set(key, "transaction");
        }, key);

        assertThat(result.discarded()).isTrue();

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    @Test
    public void blockingTxWithOptimisticLocking_success() {
        blocking.value(String.class).set(key, "hello");

        OptimisticLockingTransactionResult<String> result = blocking.withTransaction(preTx -> {
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            tx.value(String.class).set(key, input + "|foobar");
        }, key);

        assertThat(result.getPreTransactionResult()).isEqualTo("hello");
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(1);
        assertThat((Object) result.get(0)).isNull();

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello|foobar");
    }

    @Test
    public void blockingTxWithOptimisticLocking_clientFailureInPreTx() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(preTx -> {
                preTx.execute(Command.SET, key); // missing argument
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                tx.value(String.class).set(key, input + "|foobar");
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithOptimisticLocking_earlyFailureInPreTx() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(preTx -> {
                preTx.execute("nonexisting_command");
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                tx.value(String.class).set(key, input + "|foobar");
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithOptimisticLocking_casFailureInPreTx() {
        CompletableFuture<Void> begin = new CompletableFuture<>();
        CompletableFuture<Void> end = new CompletableFuture<>();

        blocking.value(String.class).set(key, "hello");

        new Thread(() -> {
            begin.join();
            blocking.value(String.class).set(key, "foobar");
            end.complete(null);
        }).start();

        OptimisticLockingTransactionResult<String> result = blocking.withTransaction(preTx -> {
            begin.complete(null);
            end.join();
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            tx.value(String.class).set(key, input + "|transaction");
        }, key);

        assertThat(result.getPreTransactionResult()).isEqualTo("foobar");
        assertThat(result.discarded()).isTrue();

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    @Test
    public void blockingTxWithOptimisticLocking_clientFailureInTx() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(preTx -> {
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                tx.value(String.class).set(key, input + "|foobar");
                tx.execute(Command.SET, key); // missing argument
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithOptimisticLocking_earlyFailureInTx() {
        blocking.value(String.class).set(key, "hello");

        assertThatThrownBy(() -> {
            blocking.withTransaction(preTx -> {
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                tx.value(String.class).set(key, input + "|foobar");
                tx.execute("nonexisting_command");
            }, key);
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello");
    }

    @Test
    public void blockingTxWithOptimisticLocking_lateFailureInTx() {
        blocking.value(String.class).set(key, "hello");

        OptimisticLockingTransactionResult<String> result = blocking.withTransaction(preTx -> {
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            tx.value(String.class).set(key, input + "|foobar");
            tx.list(String.class).lpop(key);
        }, key);

        assertThat(result.getPreTransactionResult()).isEqualTo("hello");
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1))
                .asInstanceOf(throwable(ErrorType.class))
                .hasMessageContaining("WRONGTYPE Operation against a key holding the wrong kind of value");

        assertThat(blocking.value(String.class).get(key)).isEqualTo("hello|foobar");
    }

    @Test
    public void blockingTxWithOptimisticLocking_casFailureInTx() {
        CompletableFuture<Void> begin = new CompletableFuture<>();
        CompletableFuture<Void> end = new CompletableFuture<>();

        blocking.value(String.class).set(key, "hello");

        new Thread(() -> {
            begin.join();
            blocking.value(String.class).set(key, "foobar");
            end.complete(null);
        }).start();

        OptimisticLockingTransactionResult<String> result = blocking.withTransaction(preTx -> {
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            begin.complete(null);
            end.join();
            tx.value(String.class).set(key, input + "|transaction");
        }, key);

        assertThat(result.getPreTransactionResult()).isEqualTo("hello");
        assertThat(result.discarded()).isTrue();

        assertThat(blocking.value(String.class).get(key)).isEqualTo("foobar");
    }

    // ---

    @Test
    public void reactiveTx_success() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        TransactionResult result = reactive.withTransaction(tx -> {
            return tx.value(String.class).set(key, "foobar");
        }).await().indefinitely();

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(1);
        assertThat((Object) result.get(0)).isNull();

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }

    @Test
    public void reactiveTx_clientFailure() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(tx -> {
                return tx.value(String.class).set(key, "foobar")
                        .flatMap(ignored -> tx.execute(Command.SET, key)); // missing argument
            }).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTx_earlyFailure() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(tx -> {
                return tx.value(String.class).set(key, "foobar")
                        .flatMap(ignored -> tx.execute("nonexisting_command"));
            }).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTx_lateFailure() {
        reactive.value(String.class).set(key, "hello");

        TransactionResult result = reactive.withTransaction(tx -> {
            return tx.value(String.class).set(key, "foobar")
                    .flatMap(ignored -> tx.list(String.class).lpop(key));
        }).await().indefinitely();

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1))
                .asInstanceOf(throwable(ErrorType.class))
                .hasMessageContaining("WRONGTYPE Operation against a key holding the wrong kind of value");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }

    @Test
    public void reactiveTxWithWatch_success() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        TransactionResult result = reactive.withTransaction(tx -> {
            return tx.value(String.class).set(key, "foobar");
        }, key).await().indefinitely();

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(1);
        assertThat((Object) result.get(0)).isNull();

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }

    @Test
    public void reactiveTxWithWatch_clientFailure() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(tx -> {
                return tx.value(String.class).set(key, "foobar")
                        .flatMap(ignored -> tx.execute(Command.SET, key)); // missing argument
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithWatch_earlyFailure() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(tx -> {
                return tx.value(String.class).set(key, "foobar")
                        .flatMap(ignored -> tx.execute("nonexisting_command"));
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithWatch_lateFailure() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        TransactionResult result = reactive.withTransaction(tx -> {
            return tx.value(String.class).set(key, "foobar")
                    .flatMap(ignored -> tx.list(String.class).lpop(key));
        }, key).await().indefinitely();

        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1))
                .asInstanceOf(throwable(ErrorType.class))
                .hasMessageContaining("WRONGTYPE Operation against a key holding the wrong kind of value");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }

    @Test
    public void reactiveTxWithWatch_casFailure() {
        CompletableFuture<Void> begin = new CompletableFuture<>();
        CompletableFuture<Void> end = new CompletableFuture<>();

        reactive.value(String.class).set(key, "hello").await().indefinitely();

        new Thread(() -> {
            Uni.createFrom().completionStage(begin)
                    .flatMap(ignored -> reactive.value(String.class).set(key, "foobar"))
                    .onItemOrFailure().invoke(() -> end.complete(null))
                    .await().indefinitely();
        }).start();

        TransactionResult result = reactive.withTransaction(tx -> {
            begin.complete(null);
            return Uni.createFrom().completionStage(end)
                    .flatMap(ignored -> tx.value(String.class).set(key, "transaction"));
        }, key).await().indefinitely();

        assertThat(result.discarded()).isTrue();

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_success() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        OptimisticLockingTransactionResult<String> result = reactive.withTransaction(preTx -> {
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            return tx.value(String.class).set(key, input + "|foobar");
        }, key).await().indefinitely();

        assertThat(result.getPreTransactionResult()).isEqualTo("hello");
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(1);
        assertThat((Object) result.get(0)).isNull();

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello|foobar");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_clientFailureInPreTx() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(preTx -> {
                return preTx.execute(Command.SET, key) // missing argument
                        .flatMap(ignored -> preTx.value(String.class).get(key));
            }, (input, tx) -> {
                return tx.value(String.class).set(key, input + "|foobar");
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_earlyFailureInPreTx() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(preTx -> {
                return preTx.execute("nonexisting_command")
                        .flatMap(ignored -> preTx.value(String.class).get(key));
            }, (input, tx) -> {
                return tx.value(String.class).set(key, input + "|foobar");
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_casFailureInPreTx() {
        CompletableFuture<Void> begin = new CompletableFuture<>();
        CompletableFuture<Void> end = new CompletableFuture<>();

        reactive.value(String.class).set(key, "hello").await().indefinitely();

        new Thread(() -> {
            Uni.createFrom().completionStage(begin)
                    .flatMap(ignored -> reactive.value(String.class).set(key, "foobar"))
                    .onItemOrFailure().invoke(() -> end.complete(null))
                    .await().indefinitely();
        }).start();

        OptimisticLockingTransactionResult<String> result = reactive.withTransaction(preTx -> {
            begin.complete(null);
            return Uni.createFrom().completionStage(end)
                    .flatMap(ignored -> preTx.value(String.class).get(key));
        }, (input, tx) -> {
            return tx.value(String.class).set(key, input + "|transaction");
        }, key).await().indefinitely();

        assertThat(result.getPreTransactionResult()).isEqualTo("foobar");
        assertThat(result.discarded()).isTrue();

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_clientFailureInTx() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(preTx -> {
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                return tx.value(String.class).set(key, input + "|foobar")
                        .flatMap(ignored -> tx.execute(Command.SET, key)); // missing argument
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(NoStackTraceThrowable.class)
                .hasMessageContaining("Redis command is not valid");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_earlyFailureInTx() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        assertThatThrownBy(() -> {
            reactive.withTransaction(preTx -> {
                return preTx.value(String.class).get(key);
            }, (input, tx) -> {
                return tx.value(String.class).set(key, input + "|foobar")
                        .flatMap(ignored -> tx.execute("nonexisting_command"));
            }, key).await().indefinitely();
        }).isExactlyInstanceOf(CompletionException.class)
                .hasCauseExactlyInstanceOf(ErrorType.class)
                .hasMessageContaining("ERR unknown command");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_lateFailureInTx() {
        reactive.value(String.class).set(key, "hello").await().indefinitely();

        OptimisticLockingTransactionResult<String> result = reactive.withTransaction(preTx -> {
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            return tx.value(String.class).set(key, input + "|foobar")
                    .flatMap(ignored -> tx.list(String.class).lpop(key));
        }, key).await().indefinitely();

        assertThat(result.getPreTransactionResult()).isEqualTo("hello");
        assertThat(result.discarded()).isFalse();
        assertThat(result.size()).isEqualTo(2);
        assertThat((Object) result.get(0)).isNull();
        assertThat((Object) result.get(1))
                .asInstanceOf(throwable(ErrorType.class))
                .hasMessageContaining("WRONGTYPE Operation against a key holding the wrong kind of value");

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("hello|foobar");
    }

    @Test
    public void reactiveTxWithOptimisticLocking_casFailureInTx() {
        CompletableFuture<Void> begin = new CompletableFuture<>();
        CompletableFuture<Void> end = new CompletableFuture<>();

        reactive.value(String.class).set(key, "hello").await().indefinitely();

        new Thread(() -> {
            Uni.createFrom().completionStage(begin)
                    .flatMap(ignored -> reactive.value(String.class).set(key, "foobar"))
                    .onItemOrFailure().invoke(() -> end.complete(null))
                    .await().indefinitely();
        }).start();

        OptimisticLockingTransactionResult<String> result = reactive.withTransaction(preTx -> {
            return preTx.value(String.class).get(key);
        }, (input, tx) -> {
            begin.complete(null);
            return Uni.createFrom().completionStage(end)
                    .flatMap(ignored -> tx.value(String.class).set(key, input + "|transaction"));
        }, key).await().indefinitely();

        assertThat(result.getPreTransactionResult()).isEqualTo("hello");
        assertThat(result.discarded()).isTrue();

        assertThat(reactive.value(String.class).get(key).await().indefinitely()).isEqualTo("foobar");
    }
}
