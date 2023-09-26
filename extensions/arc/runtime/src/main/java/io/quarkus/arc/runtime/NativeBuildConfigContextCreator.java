package io.quarkus.arc.runtime;

import java.util.Set;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;

public class NativeBuildConfigContextCreator implements BeanCreator<NativeBuildConfigContext> {

    @Override
    public NativeBuildConfigContext create(SyntheticCreationalContext<NativeBuildConfigContext> context) {
        Set<String> buildAndRunTimeFixed = Set.of((String[]) context.getParams().get("buildAndRunTimeFixed"));
        return new NativeBuildConfigContext() {

            @Override
            public Set<String> getBuildAndRunTimeFixed() {
                return buildAndRunTimeFixed;
            }
        };
    }

}
