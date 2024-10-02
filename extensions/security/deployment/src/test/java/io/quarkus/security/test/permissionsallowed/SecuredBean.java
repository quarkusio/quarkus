package io.quarkus.security.test.permissionsallowed;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.security.PermissionsAllowed;

@ApplicationScoped
public class SecuredBean {

    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithStringArg.class, params = "record.propertyOne")
    public String nestedRecordParam_OneTier(StringRecord record) {
        return record.propertyOne();
    }

    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithStringArg.class, params = "record.secondTier.thirdTier.propertyOne")
    public String nestedRecordParam_ThreeTiers(TopTierRecord record) {
        return record.secondTier().thirdTier().propertyOne();
    }

    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithStringArg.class, params = "simpleParam.propertyOne")
    public String nestedFieldParam_OneTier(SimpleFieldParam simpleParam) {
        return simpleParam.propertyOne;
    }

    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithStringArg.class, params = "complexParam.nestedFieldParam.simpleFieldParam.propertyOne")
    public String nestedFieldParam_ThreeTiers(ComplexFieldParam complexParam) {
        return complexParam.nestedFieldParam.simpleFieldParam.propertyOne;
    }

    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithStringArg.class, params = "obj.second.third.fourth.propertyOne")
    public String multipleNestedMethods(NestedMethodsObject obj) {
        return obj.second().third().fourth().getPropertyOne();
    }

    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithStringArg.class, params = "obj.paramField.myVal.propertyOne")
    public String combinedParam(CombinedAccessParam obj) {
        return obj.paramField.myVal().propertyOne;
    }

    @PermissionsAllowed("read")
    @PermissionsAllowed(value = "ignored", permission = CustomPermissionWithMultipleArgs.class, params = { "fourth",
            "obj.paramField.myVal.propertyOne", "first" })
    public String simpleAndNested(long first, long second, CombinedAccessParam obj, int third, int fourth, int fifth) {
        return first + "" + first;
    }

}
