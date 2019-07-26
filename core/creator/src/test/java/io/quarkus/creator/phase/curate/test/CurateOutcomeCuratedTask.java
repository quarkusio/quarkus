package io.quarkus.creator.phase.curate.test;

import io.quarkus.creator.AppCreatorException;
import io.quarkus.creator.CuratedApplicationCreator;
import io.quarkus.creator.CuratedTask;
import io.quarkus.creator.curator.CurateOutcome;

class CurateOutcomeCuratedTask implements CuratedTask<CurateOutcome> {

    public static final CurateOutcomeCuratedTask INSTANCE = new CurateOutcomeCuratedTask();

    @Override
    public CurateOutcome run(CurateOutcome outcome, CuratedApplicationCreator creator) throws AppCreatorException {
        return outcome;
    }
}
