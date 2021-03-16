package io.quarkus.container.image.s2i.deployment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.dekorate.deps.kubernetes.api.model.HasMetadata;
import io.dekorate.deps.kubernetes.api.model.KubernetesListBuilder;
import io.dekorate.deps.kubernetes.api.model.ObjectReference;
import io.dekorate.deps.openshift.api.model.ImageStreamTag;
import io.dekorate.deps.openshift.api.model.SourceBuildStrategyFluent;
import io.dekorate.deps.openshift.client.OpenShiftClient;
import io.dekorate.kubernetes.decorator.Decorator;

/**
 * This class is copied from Dekorate, with the difference that the {@code waitForImageStreamTags} method
 * take a client as the argument
 *
 * TODO: Update dekorate to take the client as an argument and then remove this class
 */
public class S2iUtils {

    /**
     * Wait for the references ImageStreamTags to become available.
     *
     * @param client The openshift client used to check the status of the ImageStream
     * @param items A list of items, possibly referencing image stream tags.
     * @param amount The max amount of time to wait.
     * @param timeUnit The time unit of the time to wait.
     * @return True if the items became available false otherwise.
     */
    public static boolean waitForImageStreamTags(OpenShiftClient client, Collection<HasMetadata> items, long amount,
            TimeUnit timeUnit) {
        if (items == null || items.isEmpty()) {
            return true;
        }
        final List<String> tags = new ArrayList<>();
        new KubernetesListBuilder()
                .withItems(new ArrayList<>(items))
                .accept(new Decorator<SourceBuildStrategyFluent>() {
                    @Override
                    public void visit(SourceBuildStrategyFluent strategy) {
                        ObjectReference from = strategy.buildFrom();
                        if (from.getKind().equals("ImageStreamTag")) {
                            tags.add(from.getName());
                        }
                    }
                }).build();

        boolean tagsMissing = true;
        long started = System.currentTimeMillis();
        long elapsed = 0;

        while (tagsMissing && elapsed < timeUnit.toMillis(amount) && !Thread.interrupted()) {
            tagsMissing = false;
            for (String tag : tags) {
                ImageStreamTag t = client.imageStreamTags().withName(tag).get();
                if (t == null) {
                    tagsMissing = true;
                }
            }

            if (tagsMissing) {
                try {
                    Thread.sleep(1000);
                    elapsed = System.currentTimeMillis() - started;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        return !tagsMissing;
    }
}
