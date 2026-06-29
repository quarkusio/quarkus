package io.quarkus.hibernate.panache.deployment.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.page.PageRequest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.hibernate.panache.blocking.PanacheBlockingQuery;
import io.quarkus.test.QuarkusExtensionTest;

public class PagingTest {

    @RegisterExtension
    static QuarkusExtensionTest runner = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-test.properties", "application.properties")
                    .addClasses(MyEntity.class, MyEntity_.class, _MyEntity.class,
                            MyEntity_.ManagedBlockingQueries_.class, MyEntity_.FindOnlyRepo_.class));

    @Inject
    MyEntity.ManagedBlockingQueries repo;

    @Transactional
    void createEntities() {
        repo.deleteAll();
        for (int i = 0; i < 25; i++) {
            MyEntity entity = new MyEntity();
            entity.foo = "foo" + String.format("%02d", i);
            entity.bar = "bar" + (24 - i);
            entity.persist();
        }
    }

    // Offset-based paging

    @Transactional
    void offsetPageBasic() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> page0 = query.pages().page(0, 10).list();
        assertThat(page0).hasSize(10);
        assertThat(page0.get(0).foo).isEqualTo("foo00");
        assertThat(page0.get(9).foo).isEqualTo("foo09");

        List<MyEntity> page1 = query.pages().page(1, 10).list();
        assertThat(page1).hasSize(10);
        assertThat(page1.get(0).foo).isEqualTo("foo10");

        List<MyEntity> page2 = query.pages().page(2, 10).list();
        assertThat(page2).hasSize(5);
        assertThat(page2.get(0).foo).isEqualTo("foo20");
    }

    @Transactional
    void offsetPageNavigation() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        query.pages().page(0, 10);
        List<MyEntity> page0 = query.list();
        assertThat(page0).hasSize(10);
        assertThat(query.pages().hasNext()).isTrue();
        assertThat(query.pages().hasPrevious()).isFalse();

        query.pages().next();
        List<MyEntity> page1 = query.list();
        assertThat(page1).hasSize(10);
        assertThat(page1.get(0).foo).isEqualTo("foo10");
        assertThat(query.pages().hasNext()).isTrue();
        assertThat(query.pages().hasPrevious()).isTrue();

        query.pages().next();
        List<MyEntity> page2 = query.list();
        assertThat(page2).hasSize(5);
        assertThat(query.pages().hasNext()).isFalse();
        assertThat(query.pages().hasPrevious()).isTrue();

        query.pages().previous();
        List<MyEntity> back = query.list();
        assertThat(back).hasSize(10);
        assertThat(back.get(0).foo).isEqualTo("foo10");

        query.pages().first();
        List<MyEntity> first = query.list();
        assertThat(first.get(0).foo).isEqualTo("foo00");

        query.pages().last();
        List<MyEntity> last = query.list();
        assertThat(last.get(0).foo).isEqualTo("foo20");
        assertThat(last).hasSize(5);
    }

    @Transactional
    void offsetPageCount() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        query.pages().page(0, 10);
        assertThat(query.pages().count()).isEqualTo(3L);
        assertThat(query.count()).isEqualTo(25L);
    }

    @Transactional
    void offsetPageIterateAll() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        int totalResults = 0;
        List<MyEntity> list = query.pages().page(0, 10).list();
        totalResults += list.size();
        while (query.pages().hasNext()) {
            list = query.pages().next().list();
            totalResults += list.size();
        }
        assertThat(totalResults).isEqualTo(25);
    }

    // Cursor-based paging

    @Transactional
    void cursorPageBasic() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> page0 = query.pages().cursor(0, 10).list();
        assertThat(page0).hasSize(10);
        assertThat(page0.get(0).foo).isEqualTo("foo00");
        assertThat(page0.get(9).foo).isEqualTo("foo09");
    }

    @Transactional
    void cursorPageNavigation() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> page0 = query.pages().cursor(0, 10).list();
        assertThat(page0).hasSize(10);
        assertThat(query.pages().hasNext()).isTrue();
        assertThat(query.pages().hasPrevious()).isFalse();

        List<MyEntity> page1 = query.pages().next().list();
        assertThat(page1).hasSize(10);
        assertThat(page1.get(0).foo).isEqualTo("foo10");
        assertThat(query.pages().hasNext()).isTrue();
        assertThat(query.pages().hasPrevious()).isTrue();

        List<MyEntity> page2 = query.pages().next().list();
        assertThat(page2).hasSize(5);
        assertThat(query.pages().hasNext()).isFalse();
        assertThat(query.pages().hasPrevious()).isTrue();

        List<MyEntity> backToPage1 = query.pages().previous().list();
        assertThat(backToPage1).hasSize(10);
        assertThat(backToPage1.get(0).foo).isEqualTo("foo10");

        List<MyEntity> first = query.pages().first().list();
        assertThat(first).hasSize(10);
        assertThat(first.get(0).foo).isEqualTo("foo00");
    }

    @Transactional
    void cursorPageIterateAll() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        int totalResults = 0;
        List<MyEntity> list = query.pages().cursor(0, 10).list();
        totalResults += list.size();
        while (query.pages().hasNext()) {
            list = query.pages().next().list();
            totalResults += list.size();
        }
        assertThat(totalResults).isEqualTo(25);
    }

    @Transactional
    void cursorPageWithoutSortThrows() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll();

        assertThatThrownBy(() -> query.pages().cursor(0, 10))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("sort");
    }

    @Transactional
    void cursorPageLastThrows() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        query.pages().cursor(0, 10).list();
        assertThatThrownBy(() -> query.pages().last())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // Limiting

    @Transactional
    void limitBasic() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limit(10).list();
        assertThat(list).hasSize(10);
        assertThat(list.get(0).foo).isEqualTo("foo00");
        assertThat(list.get(9).foo).isEqualTo("foo09");
    }

    @Transactional
    void limitWithStartOffset() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limit(5, 10).list();
        assertThat(list).hasSize(10);
        assertThat(list.get(0).foo).isEqualTo("foo05");
    }

    @Transactional
    void limitRange() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().range(5, 14).list();
        assertThat(list).hasSize(10);
        assertThat(list.get(0).foo).isEqualTo("foo05");
        assertThat(list.get(9).foo).isEqualTo("foo14");
    }

    @Transactional
    void limitAll() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limit(100).list();
        assertThat(list).hasSize(25);
    }

    @Transactional
    void limitZeroThrows() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll();

        assertThatThrownBy(() -> query.limits().limit(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Transactional
    void limitFrom() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limitFrom(5).list();
        assertThat(list).hasSize(10);
        assertThat(list.get(0).foo).isEqualTo("foo05");
        assertThat(list.get(9).foo).isEqualTo("foo14");
    }

    @Transactional
    void limitFromBeyondEnd() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limitFrom(20).list();
        assertThat(list).hasSize(5);
        assertThat(list.get(0).foo).isEqualTo("foo20");
    }

    @Transactional
    void limitWithJakartaLimit() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limit(Limit.of(10)).list();
        assertThat(list).hasSize(10);
        assertThat(list.get(0).foo).isEqualTo("foo00");
        assertThat(list.get(9).foo).isEqualTo("foo09");
    }

    @Transactional
    void limitWithJakartaLimitRange() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> list = query.limits().limit(Limit.range(6, 15)).list();
        assertThat(list).hasSize(10);
        assertThat(list.get(0).foo).isEqualTo("foo05");
        assertThat(list.get(9).foo).isEqualTo("foo14");
    }

    @Transactional
    void requestWithPageRequest() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        List<MyEntity> page0 = query.pages().request(PageRequest.ofPage(1, 10, false)).list();
        assertThat(page0).hasSize(10);
        assertThat(page0.get(0).foo).isEqualTo("foo00");
        assertThat(page0.get(9).foo).isEqualTo("foo09");

        List<MyEntity> page1 = query.pages().request(PageRequest.ofPage(2, 10, false)).list();
        assertThat(page1).hasSize(10);
        assertThat(page1.get(0).foo).isEqualTo("foo10");

        List<MyEntity> page2 = query.pages().request(PageRequest.ofPage(3, 10, false)).list();
        assertThat(page2).hasSize(5);
        assertThat(page2.get(0).foo).isEqualTo("foo20");
    }

    // Switching between paging modes

    @Transactional
    void switchFromOffsetToLimit() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        query.pages().page(0, 10);
        List<MyEntity> paged = query.list();
        assertThat(paged).hasSize(10);

        List<MyEntity> limited = query.limits().limit(5).list();
        assertThat(limited).hasSize(5);
        assertThat(limited.get(0).foo).isEqualTo("foo00");
    }

    @Transactional
    void switchFromLimitToOffset() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(Order.by(_MyEntity.foo.asc()));

        query.limits().limit(5);
        List<MyEntity> limited = query.list();
        assertThat(limited).hasSize(5);

        List<MyEntity> paged = query.pages().page(1, 10).list();
        assertThat(paged).hasSize(10);
        assertThat(paged.get(0).foo).isEqualTo("foo10");
    }

    // No paging/limiting (all results)

    @Transactional
    void noPaging() {
        PanacheBlockingQuery<MyEntity> query = repo.findAll(_MyEntity.foo.asc());

        List<MyEntity> all = query.list();
        assertThat(all).hasSize(25);
        assertThat(all.get(0).foo).isEqualTo("foo00");
        assertThat(all.get(24).foo).isEqualTo("foo24");
    }

    @Transactional
    void clear() {
        repo.deleteAll();
    }

    @Test
    void testPaging() {
        createEntities();

        // No paging
        noPaging();

        // Offset-based paging
        offsetPageBasic();
        offsetPageNavigation();
        offsetPageCount();
        offsetPageIterateAll();

        // Cursor-based paging
        cursorPageBasic();
        cursorPageNavigation();
        cursorPageIterateAll();
        cursorPageWithoutSortThrows();
        cursorPageLastThrows();

        // Limiting
        limitBasic();
        limitWithStartOffset();
        limitRange();
        limitAll();
        limitZeroThrows();
        limitFrom();
        limitFromBeyondEnd();
        limitWithJakartaLimit();
        limitWithJakartaLimitRange();

        // PageRequest
        requestWithPageRequest();

        // Switching modes
        switchFromOffsetToLimit();
        switchFromLimitToOffset();

        clear();
    }

}
