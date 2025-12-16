package io.quarkus.maven.components;

import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
@Named("quarkus")
public class QuarkusLifecycleMappingProvider extends LifecycleMappingProviderSupport {
}
