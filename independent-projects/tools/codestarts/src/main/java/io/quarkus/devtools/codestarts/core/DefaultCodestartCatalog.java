package io.quarkus.devtools.codestarts.core;

import io.quarkus.devtools.codestarts.Codestart;
import io.quarkus.devtools.codestarts.CodestartProjectInput;
import io.quarkus.devtools.codestarts.CodestartResourceLoader;
import java.util.Collection;

public final class DefaultCodestartCatalog extends GenericCodestartCatalog<CodestartProjectInput> {
    public DefaultCodestartCatalog(CodestartResourceLoader resourceLoader, Collection<Codestart> codestarts) {
        super(resourceLoader, codestarts);
    }
}
