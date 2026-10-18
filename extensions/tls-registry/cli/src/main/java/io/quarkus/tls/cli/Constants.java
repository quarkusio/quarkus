package io.quarkus.tls.cli;

import java.io.File;

public interface Constants {

    String CA_FILE_NAME = "quarkus-dev-root-ca.pem";
    String PK_FILE_NAME = "quarkus-dev-root-key.pem";
    String KEYSTORE_FILE_NAME = "quarkus-dev-keystore.p12";

    File BASE_DIR = new File(System.getenv("HOME"), ".quarkus");

    File CA_FILE = new File(BASE_DIR, CA_FILE_NAME);
    File PK_FILE = new File(BASE_DIR, PK_FILE_NAME);
    File KEYSTORE_FILE = new File(BASE_DIR, KEYSTORE_FILE_NAME);
}
