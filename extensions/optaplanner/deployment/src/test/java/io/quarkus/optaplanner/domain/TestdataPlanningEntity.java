package io.quarkus.optaplanner.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataPlanningEntity {

    private String value;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

}
