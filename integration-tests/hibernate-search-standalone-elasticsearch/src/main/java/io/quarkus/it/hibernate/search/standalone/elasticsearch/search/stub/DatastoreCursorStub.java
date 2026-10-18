/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package io.quarkus.it.hibernate.search.standalone.elasticsearch.search.stub;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DatastoreCursorStub<T> implements AutoCloseable {

    private final Iterator<T> iterator;

    public DatastoreCursorStub(Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public void close() {
        // Nothing to do, this class is more or less a stub.
    }

    public List<T> next(int count) {
        if (!iterator.hasNext()) {
            return null;
        }
        List<T> result = new ArrayList<>();
        for (int i = 0; i < count && iterator.hasNext(); i++) {
            result.add(iterator.next());
        }
        return result;
    }

}
