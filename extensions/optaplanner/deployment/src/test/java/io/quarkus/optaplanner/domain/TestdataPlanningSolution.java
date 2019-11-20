package io.quarkus.optaplanner.domain;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.solution.drools.ProblemFactCollectionProperty;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class TestdataPlanningSolution {

    private List<TestdataPlanningValue> valueList;
    private List<TestdataPlanningEntity> entityList;

    private SimpleScore score;

    @ValueRangeProvider(id = "valueRange")
    @ProblemFactCollectionProperty
    public List<TestdataPlanningValue> getValueList() {
        return valueList;
    }

    public void setValueList(List<TestdataPlanningValue> valueList) {
        this.valueList = valueList;
    }

    @PlanningEntityCollectionProperty
    public List<TestdataPlanningEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<TestdataPlanningEntity> entityList) {
        this.entityList = entityList;
    }

    @PlanningScore
    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }

}
