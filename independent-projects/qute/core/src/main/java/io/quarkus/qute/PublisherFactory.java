package io.quarkus.qute;

import org.reactivestreams.Publisher;

/**
 * Service provider.
 */
public interface PublisherFactory {

    Publisher<String> createPublisher(TemplateInstance rendering);

}
