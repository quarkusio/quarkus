package io.quarkus.arc.test.interceptors.bridge;

import javax.inject.Singleton;

import io.quarkus.arc.test.interceptors.Simple;

@Singleton
@Simple
public class ExampleResource extends AbstractResource<String> implements ExampleApi {

}
