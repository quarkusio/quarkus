package io.quarkus.mongodb.panache.runtime;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.bson.Document;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import io.quarkus.mongodb.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.exception.PanacheQueryException;

public class PanacheQueryImpl<Entity> implements PanacheQuery<Entity> {
    private MongoCollection collection;
    private Class<? extends Entity> entityClass;
    private Document mongoQuery;
    private Document sort;
    private Document projections;

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
    public <T> PanacheQuery<T> project(Class<T> type) {
        Set<String> fieldNames = new HashSet<>();
        // gather field names from getters
        for (Method method : type.getMethods()) {
            if (method.getName().startsWith("get") && !method.getName().equals("getClass")) {
                String fieldName = MongoPropertyUtil.decapitalize(method.getName().substring(3));
                fieldNames.add(fieldName);
            }
        }

        // gather field names from public fields
        for (Field field : type.getFields()) {
            fieldNames.add(field.getName());
        }

        // replace fields that have @BsonProperty mappings
        Map<String, String> replacementMap = MongoPropertyUtil.getReplacementMap(type);
        for (Map.Entry<String, String> entry : replacementMap.entrySet()) {
            if (fieldNames.contains(entry.getKey())) {
                fieldNames.remove(entry.getKey());
                fieldNames.add(entry.getValue());
            }
        }

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
        if (this.projections != null) {
            find.projection(projections);
        }
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
    public <T extends Entity> Optional<T> firstResultOptional() {
        return Optional.ofNullable(firstResult());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T singleResult() {
        List<T> list = list();
        if (list.isEmpty() || list.size() > 1) {
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
}
