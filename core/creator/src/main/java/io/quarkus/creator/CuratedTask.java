package io.quarkus.creator;

import io.quarkus.creator.curator.CurateOutcome;

/**
 * A task that requires a curated application to run
 */
public interface CuratedTask<T> {

    /**
     * Runs the curated task
     *
     * @param outcome The curate outcome
     * @return The result, possibly null
     */
    T run(CurateOutcome outcome, CuratedApplicationCreator creator) throws AppCreatorException;

}
