package io.quarkus.it.optaplanner.domain;

import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;

@PlanningSolution
public class ITestdataPlanningSolution {

    @ValueRangeProvider(id = "valueRange")
    private List<String> valueList;
    @PlanningEntityCollectionProperty
    private List<ITestdataPlanningEntity> entityList;

    @PlanningScore
    private SimpleScore score;

    // ************************************************************************
    // Getters/setters
    // ************************************************************************

    public List<String> getValueList() {
        return valueList;
    }

    public void setValueList(List<String> valueList) {
        this.valueList = valueList;
    }

    public List<ITestdataPlanningEntity> getEntityList() {
        return entityList;
    }

    public void setEntityList(List<ITestdataPlanningEntity> entityList) {
        this.entityList = entityList;
    }

    public SimpleScore getScore() {
        return score;
    }

    public void setScore(SimpleScore score) {
        this.score = score;
    }
}
