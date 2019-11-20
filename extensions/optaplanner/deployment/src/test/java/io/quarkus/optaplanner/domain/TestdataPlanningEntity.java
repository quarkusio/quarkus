package io.quarkus.optaplanner.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

@PlanningEntity
public class TestdataPlanningEntity {

    private TestdataPlanningValue value;

    @PlanningVariable(valueRangeProviderRefs = "valueRange")
    public TestdataPlanningValue getValue() {
        return value;
    }

    public void setValue(TestdataPlanningValue value) {
        this.value = value;
    }

}
