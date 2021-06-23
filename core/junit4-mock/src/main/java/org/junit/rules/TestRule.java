package org.junit.rules;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public interface TestRule {
    Statement apply(Statement var1, Description var2);
}
