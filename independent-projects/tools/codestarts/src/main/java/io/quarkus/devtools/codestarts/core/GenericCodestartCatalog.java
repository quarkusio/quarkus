package io.quarkus.devtools.codestarts.core;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartCatalog;
import io.quarkus.devtools.codestarts.CodestartProjectDefinition;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.codestarts.CodestartResourceLoader;
import io.quarkus.devtools.codestarts.CodestartsSelection;
import java.util.Collection;

public class GenericCodestartCatalog<T extends CodestartProjectInput> implements CodestartCatalog<T> {

    protected final Collection<Codestart> codestarts;
    protected final CodestartResourceLoader resourceLoader;

    public GenericCodestartCatalog(CodestartResourceLoader resourceLoader, Collection<Codestart> codestarts) {
        this.resourceLoader = resourceLoader;
        this.codestarts = codestarts;
    }

    @Override
    public Collection<Codestart> getCodestarts() {
        return codestarts;
    }

    @Override
    public CodestartProjectDefinition createProject(T projectInput) {
        final Collection<Codestart> selected = select(projectInput);
        return DefaultCodestartProjectDefinition.of(resourceLoader, projectInput, selected);
    }

    protected Collection<Codestart> select(T projectInput) {
        return select(projectInput.getSelection());
    }

    protected Collection<Codestart> select(CodestartsSelection selection) {
        return CodestartCatalogs.select(this.codestarts, selection.getNames());
    }
}
