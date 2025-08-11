package io.quarkus.it.nat.annotation;

import io.quarkus.runtime.annotations.RegisterResourceBundle;
import io.quarkus.runtime.annotations.RegisterResources;

@RegisterResources(globs = "file.txt")
@RegisterResourceBundle(bundleName = "messages")
public class NativeImageHints {
}
