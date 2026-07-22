package io.quarkus.data.hibernate.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.data.hibernate.reactive.ReactiveDataQuery;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.smallrye.mutiny.Uni;

public class ReactivePagingTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyReactiveEntity.class, MyReactiveEntity_.class, _MyReactiveEntity.class,
                            MyReactiveEntity_.ManagedReactiveQueries_.class));

    @Inject
    MyReactiveEntity.ManagedReactiveQueries repo;

    @WithTransaction
    Uni<Void> createEntities() {
        return repo.deleteAll().flatMap(v -> {
            Uni<Void> chain = Uni.createFrom().voidItem();
            for (int i = 0; i < 25; i++) {
                MyReactiveEntity entity = new MyReactiveEntity();
                entity.foo = "foo" + String.format("%02d", i);
                entity.bar = "bar" + (24 - i);
                chain = chain.flatMap(ignored -> entity.persist().replaceWithVoid());
            }
            return chain;
        });
    }

    @WithTransaction
    Uni<Void> clear() {
        return repo.deleteAll().replaceWithVoid();
    }

    // Offset-based paging

    @WithTransaction
    Uni<Void> offsetPageBasic() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.pages().page(0, 10).list().flatMap(page0 -> {
            assertThat(page0).hasSize(10);
            assertThat(page0.get(0).foo).isEqualTo("foo00");
            assertThat(page0.get(9).foo).isEqualTo("foo09");

            return query.pages().page(1, 10).list();
        }).flatMap(page1 -> {
            assertThat(page1).hasSize(10);
            assertThat(page1.get(0).foo).isEqualTo("foo10");

            return query.pages().page(2, 10).list();
        }).map(page2 -> {
            assertThat(page2).hasSize(5);
            assertThat(page2.get(0).foo).isEqualTo("foo20");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> offsetPageNavigation() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        query.pages().page(0, 10);
        return query.list().flatMap(page0 -> {
            assertThat(page0).hasSize(10);
            return query.pages().hasNext();
        }).flatMap(hasNext -> {
            assertThat(hasNext).isTrue();
            return query.pages().hasPrevious();
        }).flatMap(hasPrev -> {
            assertThat(hasPrev).isFalse();

            query.pages().next();
            return query.list();
        }).flatMap(page1 -> {
            assertThat(page1).hasSize(10);
            assertThat(page1.get(0).foo).isEqualTo("foo10");

            query.pages().next();
            return query.list();
        }).flatMap(page2 -> {
            assertThat(page2).hasSize(5);
            return query.pages().hasNext();
        }).flatMap(hasNext -> {
            assertThat(hasNext).isFalse();

            query.pages().previous();
            return query.list();
        }).flatMap(back -> {
            assertThat(back).hasSize(10);
            assertThat(back.get(0).foo).isEqualTo("foo10");

            query.pages().first();
            return query.list();
        }).flatMap(first -> {
            assertThat(first.get(0).foo).isEqualTo("foo00");

            return query.pages().last();
        }).flatMap(q -> q.list()).map(last -> {
            assertThat(last.get(0).foo).isEqualTo("foo20");
            assertThat(last).hasSize(5);
            return null;
        });
    }

    @WithTransaction
    Uni<Void> offsetPageCount() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        query.pages().page(0, 10);
        return query.pages().count().flatMap(pageCount -> {
            assertThat(pageCount).isEqualTo(3L);
            return query.count();
        }).map(totalCount -> {
            assertThat(totalCount).isEqualTo(25L);
            return null;
        });
    }

    @WithTransaction
    Uni<Void> offsetPageIterateAll() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        int[] totalResults = { 0 };
        return query.pages().page(0, 10).list().flatMap(list -> {
            totalResults[0] += list.size();
            return iteratePages(query, totalResults);
        }).map(v -> {
            assertThat(totalResults[0]).isEqualTo(25);
            return null;
        });
    }

    private Uni<Void> iteratePages(ReactiveDataQuery<MyReactiveEntity> query, int[] totalResults) {
        return query.pages().hasNext().flatMap(hasNext -> {
            if (!hasNext) {
                return Uni.createFrom().voidItem();
            }
            query.pages().next();
            return query.list().flatMap(list -> {
                totalResults[0] += list.size();
                return iteratePages(query, totalResults);
            });
        });
    }

    // Cursor-based paging (unsupported in Hibernate Reactive)

    @WithTransaction
    Uni<Void> cursorPageThrows() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        assertThatThrownBy(() -> query.pages().cursor(0, 10))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("Hibernate Reactive");
        return Uni.createFrom().voidItem();
    }

    // Limiting

    @WithTransaction
    Uni<Void> limitBasic() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limit(10).list().map(list -> {
            assertThat(list).hasSize(10);
            assertThat(list.get(0).foo).isEqualTo("foo00");
            assertThat(list.get(9).foo).isEqualTo("foo09");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitWithStartOffset() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limit(5, 10).list().map(list -> {
            assertThat(list).hasSize(10);
            assertThat(list.get(0).foo).isEqualTo("foo05");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitRange() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().range(5, 14).list().map(list -> {
            assertThat(list).hasSize(10);
            assertThat(list.get(0).foo).isEqualTo("foo05");
            assertThat(list.get(9).foo).isEqualTo("foo14");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitAll() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limit(100).list().map(list -> {
            assertThat(list).hasSize(25);
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitZeroThrows() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll();

        assertThatThrownBy(() -> query.limits().limit(0))
                .isInstanceOf(IllegalArgumentException.class);
        return Uni.createFrom().voidItem();
    }

    @WithTransaction
    Uni<Void> limitFrom() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limitFrom(5).list().map(list -> {
            assertThat(list).hasSize(10);
            assertThat(list.get(0).foo).isEqualTo("foo05");
            assertThat(list.get(9).foo).isEqualTo("foo14");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitFromBeyondEnd() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limitFrom(20).list().map(list -> {
            assertThat(list).hasSize(5);
            assertThat(list.get(0).foo).isEqualTo("foo20");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitWithJakartaLimit() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limit(Limit.of(10)).list().map(list -> {
            assertThat(list).hasSize(10);
            assertThat(list.get(0).foo).isEqualTo("foo00");
            assertThat(list.get(9).foo).isEqualTo("foo09");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> limitWithJakartaLimitRange() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limit(Limit.range(6, 15)).list().map(list -> {
            assertThat(list).hasSize(10);
            assertThat(list.get(0).foo).isEqualTo("foo05");
            assertThat(list.get(9).foo).isEqualTo("foo14");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> requestWithPageRequest() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.pages().request(PageRequest.ofPage(1, 10, false)).list().flatMap(page0 -> {
            assertThat(page0).hasSize(10);
            assertThat(page0.get(0).foo).isEqualTo("foo00");
            assertThat(page0.get(9).foo).isEqualTo("foo09");

            return query.pages().request(PageRequest.ofPage(2, 10, false)).list();
        }).flatMap(page1 -> {
            assertThat(page1).hasSize(10);
            assertThat(page1.get(0).foo).isEqualTo("foo10");

            return query.pages().request(PageRequest.ofPage(3, 10, false)).list();
        }).map(page2 -> {
            assertThat(page2).hasSize(5);
            assertThat(page2.get(0).foo).isEqualTo("foo20");
            return null;
        });
    }

    // Switching between paging modes

    @WithTransaction
    Uni<Void> switchFromOffsetToLimit() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        query.pages().page(0, 10);
        return query.list().flatMap(paged -> {
            assertThat(paged).hasSize(10);

            return query.limits().limit(5).list();
        }).map(limited -> {
            assertThat(limited).hasSize(5);
            assertThat(limited.get(0).foo).isEqualTo("foo00");
            return null;
        });
    }

    @WithTransaction
    Uni<Void> switchFromLimitToOffset() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(Order.by(_MyReactiveEntity.foo.asc()));

        return query.limits().limit(5).list().flatMap(limited -> {
            assertThat(limited).hasSize(5);

            return query.pages().page(1, 10).list();
        }).map(paged -> {
            assertThat(paged).hasSize(10);
            assertThat(paged.get(0).foo).isEqualTo("foo10");
            return null;
        });
    }

    // No paging/limiting (all results)

    @WithTransaction
    Uni<Void> noPaging() {
        ReactiveDataQuery<MyReactiveEntity> query = repo.findAll().sort(_MyReactiveEntity.foo.asc());

        return query.list().map(all -> {
            assertThat(all).hasSize(25);
            assertThat(all.get(0).foo).isEqualTo("foo00");
            assertThat(all.get(24).foo).isEqualTo("foo24");
            return null;
        });
    }

    @RunOnVertxContext
    @Test
    void testPaging(UniAsserter asserter) {
        asserter.execute(() -> createEntities());

        // No paging
        asserter.execute(() -> noPaging());

        // Offset-based paging
        asserter.execute(() -> offsetPageBasic());
        asserter.execute(() -> offsetPageNavigation());
        asserter.execute(() -> offsetPageCount());
        asserter.execute(() -> offsetPageIterateAll());

        // Cursor-based paging (unsupported in Hibernate Reactive)
        asserter.execute(() -> cursorPageThrows());

        // Limiting
        asserter.execute(() -> limitBasic());
        asserter.execute(() -> limitWithStartOffset());
        asserter.execute(() -> limitRange());
        asserter.execute(() -> limitAll());
        asserter.execute(() -> limitZeroThrows());
        asserter.execute(() -> limitFrom());
        asserter.execute(() -> limitFromBeyondEnd());
        asserter.execute(() -> limitWithJakartaLimit());
        asserter.execute(() -> limitWithJakartaLimitRange());

        // PageRequest
        asserter.execute(() -> requestWithPageRequest());

        // Switching modes
        asserter.execute(() -> switchFromOffsetToLimit());
        asserter.execute(() -> switchFromLimitToOffset());

        asserter.execute(() -> clear());
    }

}
