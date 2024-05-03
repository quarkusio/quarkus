package org.acme.examples;

import io.quarkus.builder.item.AddToMetadata;
import io.quarkus.builder.item.MultiBuildItem;

@AddToMetadata("some-cool-ability")
public final class NoteworthyBuildItem extends MultiBuildItem {

}