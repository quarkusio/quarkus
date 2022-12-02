package io.quarkus.funqy.runtime.query;

import java.lang.reflect.Type;
import java.util.HashSet;

/**
 * Value can be any primitive, primitive object, string, or bean style class
 *
 */
class QuerySetReader extends BaseCollectionReader {

    public QuerySetReader(Type genericType, QueryObjectMapper mapper) {
        super(genericType, mapper);
    }

    @Override
    public Object create() {
        return new HashSet<>();
    }

}
