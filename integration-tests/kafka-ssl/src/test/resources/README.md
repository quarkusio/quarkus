# How to generate the key and trust stores

Run:

```shell script
cd src/test/resources
rm -Rf *.p12
export SECRET=Z_pkTh9xgZovK4t34cGB2o6afT4zZg0L
export JKS_FILE=kafka-keystore.jks
export JKS_TRUST_FILE=kafka-truststore.jks
export CERT_FILE=localhost.crt
export PKCS_FILE=kafka-keystore.p12
export PKCS_TRUST_FILE=kafka-truststore.p12
export PEM_FILE_CERT=kafka-cert.pem
export PEM_FILE_KEY=kafka-key.pem
keytool -genkey -alias kafka-test-store -keyalg RSA -keystore ${JKS_FILE} -keysize 2048 -validity 1095 -dname CN=localhost -keypass ${SECRET} -storepass ${SECRET}
keytool -export -alias kafka-test-store -file ${CERT_FILE} -keystore ${JKS_FILE} -keypass ${SECRET} -storepass ${SECRET}
keytool -importkeystore -srckeystore ${JKS_FILE} -srcstorepass ${SECRET} -destkeystore ${PKCS_FILE} -deststoretype PKCS12 -deststorepass ${SECRET}
keytool -keystore ${JKS_TRUST_FILE} -import -file ${CERT_FILE} -keypass ${SECRET} -storepass ${SECRET} -noprompt  
keytool -importkeystore -srckeystore ${JKS_TRUST_FILE} -srcstorepass ${SECRET} -destkeystore ${PKCS_TRUST_FILE} -deststoretype PKCS12 -deststorepass ${SECRET}
rm -Rf localhost.crt *.jks
cd ../../.. || exit -1 
```