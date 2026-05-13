package io.quarkus.hibernate.panache;

public class MetamodelTest {
    // this only tests that we generated the metamodel, no need to run anything.
    public void test() {
        String orm = WithId_.ID;
        String jd = _WithId.ID;

        // FIXME: .Long, .String, .UUID when we switch to ORM 7
    }
}
