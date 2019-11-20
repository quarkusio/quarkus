package io.quarkus.optaplanner.constraints;

import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.score.stream.Constraint;
import org.optaplanner.core.api.score.stream.ConstraintFactory;
import org.optaplanner.core.api.score.stream.ConstraintProvider;
import org.optaplanner.core.api.score.stream.Joiners;

public class TestdataPlanningConstraintProvider implements ConstraintProvider {

    @Override
    public Constraint[] defineConstraints(ConstraintFactory factory) {
        return new Constraint[] {
                factory.from(TestdataPlanningEntity.class)
                    .join(TestdataPlanningEntity.class, Joiners.equal(TestdataPlanningEntity::getValue))
                    .penalize("Don't assign 2 entities the same value.", SimpleScore.ONE)
        };
    }

}
