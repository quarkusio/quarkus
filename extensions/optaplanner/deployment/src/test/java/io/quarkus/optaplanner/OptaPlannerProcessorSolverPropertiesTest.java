package io.quarkus.optaplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Duration;

import javax.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.optaplanner.core.api.score.buildin.simple.SimpleScore;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.config.solver.EnvironmentMode;
import org.optaplanner.core.config.solver.SolverConfig;

import io.quarkus.optaplanner.constraints.TestdataPlanningConstraintProvider;
import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import io.quarkus.test.QuarkusUnitTest;

public class OptaPlannerProcessorSolverPropertiesTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .overrideConfigKey("quarkus.optaplanner.solver.environment-mode",
                    "FULL_ASSERT")
            .overrideConfigKey("quarkus.optaplanner.solver.move-thread-count", "2")
            .overrideConfigKey("quarkus.optaplanner.solver.termination.spent-limit", "4h")
            .overrideConfigKey("quarkus.optaplanner.solver.termination.unimproved-spent-limit", "5h")
            .overrideConfigKey("quarkus.optaplanner.solver.termination.best-score-limit", "0")
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataPlanningEntity.class, TestdataPlanningSolution.class,
                            TestdataPlanningConstraintProvider.class));

    @Inject
    SolverConfig solverConfig;
    @Inject
    SolverFactory<TestdataPlanningSolution> solverFactory;

    @Test
    public void solverProperties() {
        assertEquals(EnvironmentMode.FULL_ASSERT, solverConfig.getEnvironmentMode());
        assertEquals("2", solverConfig.getMoveThreadCount());

        assertNotNull(solverFactory);
    }

    @Test
    public void terminationProperties() {
        assertEquals(Duration.ofHours(4), solverConfig.getTerminationConfig().getSpentLimit());
        assertEquals(Duration.ofHours(5), solverConfig.getTerminationConfig().getUnimprovedSpentLimit());
        assertEquals(SimpleScore.of(0).toString(), solverConfig.getTerminationConfig().getBestScoreLimit());
    }
}
