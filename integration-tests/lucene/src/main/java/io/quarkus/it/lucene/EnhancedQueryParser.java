package io.quarkus.it.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

/**
 * Query parser that take into accounts the fields in the {@link Person} class
 * and creates correct range queries
 */
public class EnhancedQueryParser extends QueryParser {

    public EnhancedQueryParser(String field, Analyzer a) {
        super(field, a);
    }

    @Override
    protected Query getRangeQuery(String field, String lower, String upper, boolean startInclusive, boolean endInclusive)
            throws ParseException {
        if ("age".equals(field)) {
            return IntPoint.newRangeQuery("age", Integer.parseInt(lower), Integer.parseInt(upper));
        }

        return super.getRangeQuery(field, lower, upper, startInclusive, endInclusive);
    }
}