package io.quarkus.test.junit5.virtual.internal.ignore;

import io.quarkus.test.junit5.virtual.ShouldPin;
import io.quarkus.test.junit5.virtual.VirtualThreadUnit;

@VirtualThreadUnit
@ShouldPin // You can use @ShouldNotPin or @ShouldPin on the super class itself, it's applied to each method.
public abstract class LoomUnitExampleShouldPinOnSuperClass {
}
