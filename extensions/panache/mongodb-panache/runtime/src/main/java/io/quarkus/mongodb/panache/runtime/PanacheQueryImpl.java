package io.quarkus.mongodb.panache.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Range;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {
    private MongoCollection collection;
    private Document mongoQuery;
    private Document sort;
    private Document projections;

    /*
     * We store the pageSize and apply it for each request because getFirstResult()
     * sets the page size to 1
     */
    private Page page;
    private Long count;

    private Range range;

    PanacheQueryImpl(MongoCollection<? extends Entity> collection, Document mongoQuery, Document sort) {
        this.collection = collection;
        this.mongoQuery = mongoQuery;
        this.sort = sort;
        page = new Page(0, Integer.MAX_VALUE);
    }

    // Builder

    @Override
    public <T> PanacheQuery<T> project(Class<T> type) {
        // collect field names from public fields and getters
        Set<String> fieldNames = MongoPropertyUtil.collectFields(type);

        // create the projection document
        this.projections = new Document();
        for (String fieldName : fieldNames) {
            this.projections.append(fieldName, 1);
        }

        return (PanacheQuery<T>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> PanacheQuery<T> page(Page page) {
        this.page = page;
        this.range = null; // reset the range to be able to switch from range to page
        return (PanacheQuery<T>) this;
    }

    @Override
    public <T extends Entity> PanacheQuery<T> page(int pageIndex, int pageSize) {
        return page(Page.of(pageIndex, pageSize));
    }

    @Override
    public <T extends Entity> PanacheQuery<T> nextPage() {
        checkNotInRange();
        return page(page.next());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> previousPage() {
        checkNotInRange();
        return page(page.previous());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> firstPage() {
        checkNotInRange();
        return page(page.first());
    }

    @Override
    public <T extends Entity> PanacheQuery<T> lastPage() {
        checkNotInRange();
        return page(page.index(pageCount() - 1));
    }

    @Override
    public boolean hasNextPage() {
        checkNotInRange();
        return page.index < (pageCount() - 1);
    }

    @Override
    public boolean hasPreviousPage() {
        checkNotInRange();
        return page.index > 0;
    }

    @Override
    public int pageCount() {
        checkNotInRange();
        long count = count();
        if (count == 0)
            return 1; // a single page of zero results
        return (int) Math.ceil((double) count / (double) page.size);
    }

    @Override
    public Page page() {
        checkNotInRange();
        return page;
    }

    private void checkNotInRange() {
        if (range != null) {
            throw new UnsupportedOperationException("Cannot call a page related method in a ranged query, " +
                    "call page(Page) or page(int, int) to initiate pagination first");
        }
    }

    @Override
    public <T extends Entity> PanacheQuery<T> range(int startIndex, int lastIndex) {
        this.range = Range.of(startIndex, lastIndex);
        // reset the page to its default to be able to switch from page to range
        this.page = new Page(0, Integer.MAX_VALUE);
        return (PanacheQuery<T>) this;
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
        if (this.projections != null) {
            find.projection(projections);
        }
        manageOffsets(find);
        MongoCursor<T> cursor = find.sort(sort).iterator();

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
    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        List<T> list = list();
        if (list.size() != 1) {
            throw new PanacheQueryException("There should be only one result");
        }

        return list.get(0);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> Optional<T> singleResultOptional() {
        List<T> list = list();
        if (list.size() > 1) {
            throw new PanacheQueryException("There should be no more than one result");
        }

        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    private void manageOffsets(FindIterable find) {
        if (range != null) {
            // range is 0 based, so we add 1 to the limit
            find.skip(range.getStartIndex()).limit(range.getLastIndex() - range.getStartIndex() + 1);
        } else {
            find.skip(page.index * page.size).limit(page.size);
        }
    }
}
