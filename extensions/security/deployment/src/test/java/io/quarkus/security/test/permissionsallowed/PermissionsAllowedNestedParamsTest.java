package io.quarkus.security.test.permissionsallowed;

import static io.quarkus.security.test.permissionsallowed.CustomPermissionWithMultipleArgs.EXPECTED_FIELD_INT_ARGUMENT;
import static io.quarkus.security.test.permissionsallowed.CustomPermissionWithMultipleArgs.EXPECTED_FIELD_LONG_ARGUMENT;
import static io.quarkus.security.test.permissionsallowed.CustomPermissionWithStringArg.EXPECTED_FIELD_STRING_ARGUMENT;
import static io.quarkus.security.test.utils.IdentityMock.USER;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertFailureFor;
import static io.quarkus.security.test.utils.SecurityTestUtils.assertSuccess;

import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.security.ForbiddenException;
import io.quarkus.security.StringPermission;
import io.quarkus.security.test.utils.AuthData;
import io.quarkus.security.test.utils.IdentityMock;
import io.quarkus.security.test.utils.SecurityTestUtils;
import io.quarkus.test.QuarkusUnitTest;

public class PermissionsAllowedNestedParamsTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(IdentityMock.class, AuthData.class, SecurityTestUtils.class, StringRecord.class,
                            SecuredBean.class, CustomPermissionWithStringArg.class, TopTierRecord.class, SimpleFieldParam.class,
                            ComplexFieldParam.class, NestedMethodsObject.class, CombinedAccessParam.class,
                            CustomPermissionWithMultipleArgs.class));

    @Inject
    SecuredBean securedBean;

    @Test
    public void testNestedRecordParam_NestingLevelOne() {
        assertSuccess(() -> securedBean.nestedRecordParam_OneTier(new StringRecord(EXPECTED_FIELD_STRING_ARGUMENT)), USER);
        assertFailureFor(() -> securedBean.nestedRecordParam_OneTier(new StringRecord("unexpected_value")),
                ForbiddenException.class, USER);
    }

    @Test
    public void testNestedRecordParam_NestingLevelThree() {
        var validTopTierRecord = new TopTierRecord(
                new TopTierRecord.SecondTierRecord(null, new StringRecord(EXPECTED_FIELD_STRING_ARGUMENT)), -1);
        assertSuccess(() -> securedBean.nestedRecordParam_ThreeTiers(validTopTierRecord), USER);
        var invalidTopTierRecord = new TopTierRecord(
                new TopTierRecord.SecondTierRecord(null, new StringRecord("unexpected_value")), -1);
        assertFailureFor(() -> securedBean.nestedRecordParam_ThreeTiers(invalidTopTierRecord), ForbiddenException.class, USER);
    }

    @Test
    public void testNestedFieldParam_NestingLevelOne() {
        assertSuccess(() -> securedBean.nestedFieldParam_OneTier(new SimpleFieldParam(EXPECTED_FIELD_STRING_ARGUMENT)), USER);
        assertFailureFor(() -> securedBean.nestedFieldParam_OneTier(new SimpleFieldParam("unexpected_value")),
                ForbiddenException.class, USER);
    }

    @Test
    public void testNestedFieldParam_NestingLevelThree() {
        var validComplexParam = new ComplexFieldParam(
                new ComplexFieldParam.NestedFieldParam(new SimpleFieldParam(EXPECTED_FIELD_STRING_ARGUMENT)));
        assertSuccess(() -> securedBean.nestedFieldParam_ThreeTiers(validComplexParam), USER);
        var invalidComplexParam = new ComplexFieldParam(
                new ComplexFieldParam.NestedFieldParam(new SimpleFieldParam("unexpected_value")));
        assertFailureFor(() -> securedBean.nestedFieldParam_ThreeTiers(invalidComplexParam),
                ForbiddenException.class, USER);
    }

    @Test
    public void multipleNestedMethods() {
        var validNestedMethods = new NestedMethodsObject(EXPECTED_FIELD_STRING_ARGUMENT);
        assertSuccess(() -> securedBean.multipleNestedMethods(validNestedMethods), USER);
        var invalidNestedMethods = new NestedMethodsObject("unexpected_value");
        assertFailureFor(() -> securedBean.multipleNestedMethods(invalidNestedMethods), ForbiddenException.class, USER);
    }

    @Test
    public void combinedFieldAndMethodAccess() {
        var validCombinedParam = new CombinedAccessParam(new CombinedAccessParam.ParamField(EXPECTED_FIELD_STRING_ARGUMENT));
        assertSuccess(() -> securedBean.combinedParam(validCombinedParam), USER);
        var invalidCombinedParam = new CombinedAccessParam(new CombinedAccessParam.ParamField("unexpected_value"));
        assertFailureFor(() -> securedBean.combinedParam(invalidCombinedParam), ForbiddenException.class, USER);
    }

    @Test
    public void simpleAndNestedParamCombination() {
        var readPerm = new AuthData(Set.of(), false, "ignored", Set.of(new StringPermission("read")));
        var noReadPerm = new AuthData(Set.of(), false, "ignored", Set.of(new StringPermission("write")));
        var validCombinedParam = new CombinedAccessParam(new CombinedAccessParam.ParamField(EXPECTED_FIELD_STRING_ARGUMENT));
        // succeed as all params are correct
        assertSuccess(() -> securedBean.simpleAndNested(EXPECTED_FIELD_LONG_ARGUMENT, -1, validCombinedParam, -2,
                EXPECTED_FIELD_INT_ARGUMENT, -3), readPerm);
        // fail as String permission is wrong
        assertFailureFor(() -> securedBean.simpleAndNested(EXPECTED_FIELD_LONG_ARGUMENT, -1, validCombinedParam, -2,
                EXPECTED_FIELD_INT_ARGUMENT, -3), ForbiddenException.class, noReadPerm);
        // fail as long param is wrong
        assertFailureFor(() -> securedBean.simpleAndNested(0, -1, validCombinedParam, -2, EXPECTED_FIELD_INT_ARGUMENT, -3),
                ForbiddenException.class, readPerm);
        // fail as int param is wrong
        assertFailureFor(() -> securedBean.simpleAndNested(EXPECTED_FIELD_LONG_ARGUMENT, -1, validCombinedParam, -2, -9, -3),
                ForbiddenException.class, readPerm);
        // fail as combined param is wrong
        var invalidCombinedParam = new CombinedAccessParam(new CombinedAccessParam.ParamField("unexpected_value"));
        assertFailureFor(() -> securedBean.simpleAndNested(EXPECTED_FIELD_LONG_ARGUMENT, -1, invalidCombinedParam, -2,
                EXPECTED_FIELD_INT_ARGUMENT, -3), ForbiddenException.class, readPerm);
    }
}
