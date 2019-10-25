package ${package_name};

import io.quarkus.test.junit.SubstrateTest;

@SubstrateTest
public class Native${class_name}IT extends ${class_name}Test {

    // Execute the same tests but in native mode.
}
