package io.quarkus.data.hibernate;

public class MetamodelTest {
    // this only tests that we generated the metamodel, no need to run anything.
    public void test() {
        String ormCustom = WithId_.ID;
        String ormLong = WithId_.AutoLong_.ID;
        String ormString = WithId_.AutoString_.ID;
        String ormUUID = WithId_.AutoUUID_.ID;

        String jdCustom = _WithId.ID;
        String jdLong = _WithId._AutoLong.ID;
        String jdString = _WithId._AutoString.ID;
        String jdUUID = _WithId._AutoUUID.ID;
    }
}
