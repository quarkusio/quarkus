package io.quarkus.devtools.codestarts.core;

import java.util.Collection;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartCatalog;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartProjectInput;

public class GenericCodestartCatalog<T extends CodestartProjectInput> implements CodestartCatalog<T> {

    protected final Collection<Codestart> codestarts;

    public GenericCodestartCatalog(Collection<Codestart> codestarts) {
        this.codestarts = codestarts;
    }

    @Override
    public Collection<Codestart> getCodestarts() {
        return codestarts;
    }

    @Override
    public CodestartProjectDefinition createProject(T projectInput) {
        final Collection<Codestart> selected = select(projectInput);
        return DefaultCodestartProjectDefinition.of(projectInput, selected);
    }

    protected Collection<Codestart> select(T projectInput) {
        return CodestartCatalogs.select(projectInput, this.codestarts);
    }

}
