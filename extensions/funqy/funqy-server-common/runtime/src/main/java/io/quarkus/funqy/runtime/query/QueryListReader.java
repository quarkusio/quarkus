package io.quarkus.funqy.runtime.query;

import java.lang.reflect.Type;
import java.util.LinkedList;

/**
 * Value can be any primitive, primitive object, string, or bean style class
 *
 */
class QueryListReader extends BaseCollectionReader {

    public QueryListReader(Type genericType, QueryObjectMapper mapper) {
        super(genericType, mapper);
    }

    @Override
    public Object create() {
        return new LinkedList<>();
    }

}
