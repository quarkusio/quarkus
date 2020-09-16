package io.quarkus.elasticsearch.restclient.highlevel.runtime;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.CheckedConsumer;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;

/**
 * The RestHighLevelClient cannot be built with an existing RestClient.
 * <p>
 * The only (and documented - see javadoc) way to do it is to subclass it and use its protected constructor.
 */
class QuarkusRestHighLevelClient extends RestHighLevelClient {

    protected QuarkusRestHighLevelClient(RestClient restClient, CheckedConsumer<RestClient, IOException> doClose,
            List<NamedXContentRegistry.Entry> namedXContentEntries) {
        super(restClient, doClose, namedXContentEntries);
    }
}
