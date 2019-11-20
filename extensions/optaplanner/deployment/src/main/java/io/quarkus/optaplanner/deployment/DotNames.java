package io.quarkus.optaplanner.deployment;

import org.jboss.jandex.DotName;
import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.solution.PlanningSolution;

public final class DotNames {

    static final DotName PLANNING_SOLUTION = DotName.createSimple(PlanningSolution.class.getName());
    static final DotName PLANNING_ENTITY = DotName.createSimple(PlanningEntity.class.getName());

    private DotNames() {
    }

}
