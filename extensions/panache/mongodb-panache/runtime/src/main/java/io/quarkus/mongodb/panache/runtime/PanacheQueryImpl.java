package io.quarkus.mongodb.panache.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {
    private MongoCollection collection;
    private Class<? extends Entity> entityClass;
    private Document mongoQuery;
    private Document sort;

    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private Page page;
    private Long count;

    PanacheQueryImpl(MongoCollection<? extends Entity> collection, Class<? extends Entity> entityClass, Document mongoQuery,
            Document sort) {
        this.collection = collection;
        this.entityClass = entityClass;
        this.mongoQuery = mongoQuery;
        this.sort = sort;
        page = new Page(0, Integer.MAX_VALUE);
    }

    // Builder

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        this.page = page;
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> PanacheQuery<T> nextPage() {
        return page(page.next());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> previousPage() {
        return page(page.previous());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> firstPage() {
        return page(page.first());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> lastPage() {
        return page(page.index(pageCount() - 1));
    }

    @Override
    public boolean hasNextPage() {
        return page.index < (pageCount() - 1);
    }

    @Override
    public boolean hasPreviousPage() {
        return page.index > 0;
    }

    @Override
    public int pageCount() {
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    @Override
    public Page page() {
        return page;
    }

    // Results

    @Override
    @SuppressWarnings("unchecked")
    public long count() {
        if (count == null) {
            count = collection.countDocuments(mongoQuery);
        }
        return count;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> List<T> list() {
        List<T> list = new ArrayList<>();
        FindIterable find = mongoQuery == null ? collection.find() : collection.find(mongoQuery);
        MongoCursor<T> cursor = find.sort(sort).skip(page.index).limit(page.size).iterator();

        try {
            while (cursor.hasNext()) {
                T entity = cursor.next();
                list.add(entity);
            }
        } finally {
            cursor.close();
        }
        return list;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Stream<T> stream() {
        return (Stream<T>) list().stream();
    }

    @Override
    public <T extends Entity> T firstResult() {
        List<T> list = list();
        return list.isEmpty() ? null : list.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        List<T> list = list();
        if (list.isEmpty() || list.size() > 1) {
            throw new RuntimeException("There should be only one result");//TODO use proper exception
        }

        return list.get(0);
    }
}
