package io.quarkus.qute.rxjava;

import io.quarkus.qute.PublisherFactory;
import io.quarkus.qute.TemplateInstance;
import io.reactivex.Flowable;
import io.reactivex.processors.UnicastProcessor;
import org.reactivestreams.Publisher;

public class RxjavaPublisherFactory implements PublisherFactory {

    @Override
    public Publisher<String> createPublisher(TemplateInstance rendering) {
        return Flowable.defer(() -> {
            UnicastProcessor<String> processor = UnicastProcessor.create();
            rendering.consume(s -> processor.onNext(s))
                    .whenComplete((v, t) -> {
                        if (t == null) {
                            processor.onComplete();
                        } else {
                            processor.onError(t);
                        }
                    });
            return processor;
        });
    }

}
