package io.quarkus.optaplanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;

import javax.inject.Inject;

import io.quarkus.optaplanner.constraints.TestdataPlanningConstraintProvider;
import io.quarkus.optaplanner.domain.TestdataPlanningEntity;
import io.quarkus.optaplanner.domain.TestdataPlanningSolution;
import io.quarkus.optaplanner.domain.TestdataPlanningValue;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;

public class SolverFactoryInjectTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(TestdataPlanningValue.class, TestdataPlanningEntity.class,
                            TestdataPlanningSolution.class, TestdataPlanningConstraintProvider.class));

    @Inject
    SolverFactory<TestdataPlanningSolution> solverFactory;

    @Test
    public void testRuleEvaluation() {
        Solver<TestdataPlanningSolution> solver = solverFactory.buildSolver();
        assertNotNull(solver);
    }

}
