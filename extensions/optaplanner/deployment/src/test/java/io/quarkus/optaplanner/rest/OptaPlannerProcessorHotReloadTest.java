package io.quarkus.optaplanner.rest;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.optaplanner.constraints.TestdataPlanningConstraintProvider;
import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class OptaPlannerProcessorHotReloadTest {

    // This fails in IntelliJ with "Undeclared build item class", but not in maven. That's normal in Quarkus for now.
    @RegisterExtension
    static final QuarkusDevModeTest test = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataPlanningEntity.class,
                            TestdataPlanningSolution.class, TestdataPlanningConstraintProvider.class,
                            SolverConfigTestResource.class)
                    .addAsResource("solverConfig.xml"));

    @Test
    public void solverConfigHotReload() {
        String resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        Assertions.assertEquals("secondsSpentLimit=2", resp);
        test.modifyResourceFile("solverConfig.xml", s -> s.replace("<secondsSpentLimit>2</secondsSpentLimit>",
                "<secondsSpentLimit>9</secondsSpentLimit>"));
        resp = RestAssured.get("/solver-config/seconds-spent-limit").asString();
        Assertions.assertEquals("secondsSpentLimit=9", resp);
    }

}
