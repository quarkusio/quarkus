package io.quarkus.axon.deployment.aggregatetest;

import javax.enterprise.context.Dependent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

@Dependent
public class TestRepository {
    private Log log = LogFactory.getLog(TestRepository.class);

    public void save() {
        log.info("Test repository save");
    }
}
