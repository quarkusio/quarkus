package io.quarkus.tls.cli.letsencrypt;

import java.io.File;

public interface LetsEncryptConstants {

    File LETS_ENCRYPT_DIR = new File(".letsencrypt");

    String CERT_FILE_NAME = "lets-encrypt.crt";
    String KEY_FILE_NAME = "lets-encrypt.key";
    String CA_FILE_NAME = "lets-encrypt-ca.crt";

    File CERT_FILE = new File(LETS_ENCRYPT_DIR, CERT_FILE_NAME);
    File KEY_FILE = new File(LETS_ENCRYPT_DIR, KEY_FILE_NAME);
    File CA_FILE = new File(LETS_ENCRYPT_DIR, CA_FILE_NAME);

    File DOT_ENV_FILE = new File(".env");;

}
