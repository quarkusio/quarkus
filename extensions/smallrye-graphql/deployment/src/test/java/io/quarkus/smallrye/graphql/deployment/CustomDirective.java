package io.quarkus.smallrye.graphql.deployment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.eclipse.microprofile.graphql.Description;
import org.eclipse.microprofile.graphql.NonNull;

import io.smallrye.graphql.api.Directive;
import io.smallrye.graphql.api.DirectiveLocation;

@Directive(on = { DirectiveLocation.OBJECT, DirectiveLocation.INTERFACE, DirectiveLocation.FIELD_DEFINITION })
@Retention(RetentionPolicy.RUNTIME)
@Description("test-description")
public @interface CustomDirective {
    @NonNull
    String[] fields();
}
