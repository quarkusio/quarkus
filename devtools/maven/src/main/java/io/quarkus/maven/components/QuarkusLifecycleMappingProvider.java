package io.quarkus.maven.components;

import javax.inject.Named;

import org.apache.maven.SessionScoped;

@SessionScoped
@Named("quarkus")
public class QuarkusLifecycleMappingProvider extends LifecycleMappingProviderSupport {
}
