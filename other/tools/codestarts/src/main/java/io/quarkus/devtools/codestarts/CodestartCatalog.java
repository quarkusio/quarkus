package io.quarkus.devtools.codestarts;

import java.util.Collection;

public interface CodestartCatalog<T extends CodestartProjectInput> {
    Collection<Codestart> getCodestarts();

    CodestartProjectDefinition createProject(T projectInput);

}
