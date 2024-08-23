package io.quarkus.it.main;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

@QuarkusTest
@TestProfile(QuarkusTestNestedWithTestProfileTestCase.OuterProfile.class)
public class QuarkusTestNested1WithCommonCaseTest extends CommonNestedTest {

    @Override
    public String defaultProfile() {
        return "OuterProfile";
    }
}
