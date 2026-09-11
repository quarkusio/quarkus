package org.acme.gradledemo;

final class DemoBuildMessage {

    private DemoBuildMessage() {
    }

    static String message() {
        return "${demoBuildMessage}";
    }
}
