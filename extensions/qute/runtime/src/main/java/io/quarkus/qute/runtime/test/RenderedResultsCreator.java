package io.quarkus.qute.runtime.test;

import io.quarkus.arc.BeanCreator;
import io.quarkus.arc.SyntheticCreationalContext;
import io.quarkus.qute.RenderedResults;

public class RenderedResultsCreator implements BeanCreator<RenderedResults> {

    @Override
    public RenderedResults create(SyntheticCreationalContext<RenderedResults> context) {
        return new RenderedResults();
    }

}
