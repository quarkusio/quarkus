package io.quarkus.optaplanner;

import javax.inject.Inject;

import io.quarkus.optaplanner.constraints.TestdataPlanningConstraintProvider;
import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import io.quarkus.optaplanner.domain.TestdataPlanningValue;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.core.impl.phase.custom.CustomPhaseCommand;
import org.optaplanner.core.impl.score.director.ScoreDirector;
import org.optaplanner.core.impl.solver.DefaultSolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class SolverFactoryInjectWithXMLTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataPlanningValue.class, TestdataPlanningEntity.class,
                            TestdataPlanningSolution.class, TestdataPlanningConstraintProvider.class)
                    .addAsResource("solverConfig.xml"));

    @Inject
    SolverFactory<TestdataPlanningSolution> solverFactory;

    @Test
    public void testRuleEvaluation() {
        Solver<TestdataPlanningSolution> solver = solverFactory.buildSolver();
        assertNotNull(solver);
        assertEquals(1, ((DefaultSolver) solver).getPhaseList().size());
    }

}
