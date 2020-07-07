package io.quarkus.elasticsearch.highlevel.client;

import java.io.IOException;
import java.util.List;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.CheckedConsumer;
import org.elasticsearch.common.xcontent.NamedXContentRegistry;

/**
 * The RestHighLevelClient didn't allow to be built with an existing RestClient.
 * The only way (and documented inside its JavaDoc), is to subclass it and uses its protected constructor.
 */
class QuarkusRestHighLevelClient extends RestHighLevelClient {
    protected QuarkusRestHighLevelClient(RestClient restClient, CheckedConsumer<RestClient, IOException> doClose,
            List<NamedXContentRegistry.Entry> namedXContentEntries) {
        super(restClient, doClose, namedXContentEntries);
    }
}
