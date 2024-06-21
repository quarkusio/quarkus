package io.quarkus.elasticsearch.restclient.lowlevel.runtime;

import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;

import java.util.Optional;

public record RestClientHolder(RestClient client, Optional<Sniffer> sniffer) {
}
