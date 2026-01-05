package io.quarkus.hibernate.reactive.panache;

public class MetamodelTest {
    // this only tests that we generated the metamodel, no need to run anything.
    public void test() {
        String orm = PanacheEntity_.ID;
        String jd = _PanacheEntity.ID;
    }
}
