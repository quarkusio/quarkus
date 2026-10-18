package io.quarkus.it.mockbean.hello;

record RecordB(RecordA recA) {
    public String dataHello() {
        return recA.data();
    }
}
