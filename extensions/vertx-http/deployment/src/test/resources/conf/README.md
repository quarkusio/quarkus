# Generate the Self signed PEM, JKS and P12 certificates

To renew the certificate, run the following script:

```bash
export SECRET=secret

# 1. Create RSA private keys and certificates as a key store and export
export JKS_FILE=server-keystore.jks
export CERT_FILE=localhost.crt
export PKCS_FILE=server-keystore.p12
export PEM_FILE_CERT=server-cert.pem
export PEM_FILE_KEY=server-key.pem
keytool -genkey -alias test-store -keyalg RSA -keystore ${JKS_FILE} -keysize 2048 -validity 1095 -dname CN=localhost -keypass ${SECRET} -storepass ${SECRET}
keytool -export -alias test-store -file ${CERT_FILE} -keystore ${JKS_FILE} -keypass ${SECRET} -storepass ${SECRET}


#2. Transform JSK into PKCS12
keytool -importkeystore -srckeystore ${JKS_FILE} -srcstorepass ${SECRET} -destkeystore ${PKCS_FILE} -deststoretype PKCS12 -deststorepass ${SECRET}

# 3. Export the PKCS12 into PEM files
openssl pkcs12 -in ${PKCS_FILE} -nodes -passin pass:${SECRET} | openssl pkcs8 -topk8 -inform PEM -outform PEM -out ${PEM_FILE_KEY} -nocrypt
openssl pkcs12 -name test-store -in ${PKCS_FILE} -nokeys -passin pass:${SECRET} -out ${PEM_FILE_CERT}  | echo test-store

```
