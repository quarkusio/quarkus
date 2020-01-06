package io.quarkus.qute.mutiny;

import io.quarkus.qute.PublisherFactory;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.mutiny.Multi;
import org.reactivestreams.Publisher;

public class MutinyPublisherFactory implements PublisherFactory {

    @Override
    public Publisher<String> createPublisher(TemplateInstance rendering) {
        return Multi.createFrom().emitter(emitter -> rendering
                .consume(emitter::emit)
                .whenComplete((r, f) -> {
                    if (f == null) {
                        emitter.complete();
                    } else {
                        emitter.fail(f);
                    }
                }));
    }

}
