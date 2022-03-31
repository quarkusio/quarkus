package io.quarkus.resteasy.reactive.server.spi;

import io.quarkus.builder.item.MultiBuildItem;

/**
 * A marker build item which extensions can generate when they want to force RESTEasy Reactive to not
 * reply with 404 when it does not handle the path and instead just pass control onto the next
 * Vert.x handler
 */
public final class ResumeOn404BuildItem extends MultiBuildItem {
}
