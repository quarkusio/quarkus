#! /bin/bash
# generate test certs
# password is always "password", there are a few commands that didn't accept a pw on the commandline

rm client.jks server.jks server2.jks

# root ca certificate
cp openssl.cnf.head openssl.cnf
openssl genrsa -out rootCA.key 2048
openssl req -x509 -new -nodes -key rootCA.key -sha256 -days 1095 -out rootCA.pem -config openssl.cnf
keytool -importcert -storetype jks -storepass password -keystore client.jks -file rootCA.pem -alias rootca

# certificate with alt names localhost, 127.0.0.1 and ::1
cp client.jks server.jks
cat openssl.cnf.head altnames_ip >openssl.cnf
openssl genrsa -out server.key 2048
openssl req -new -key server.key -out server.csr -config openssl.cnf
openssl x509 -req -in server.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server.crt -days 1095 -sha256 -extensions req_ext -extfile openssl.cnf
openssl pkcs12 -export -inkey server.key -in server.crt -certfile rootCA.pem -out server.p12
keytool -importkeystore -srckeystore server.p12 -srcstorepass password -destkeystore server.jks -deststorepass password -srcstoretype pkcs12 -deststoretype jks

# certificate with alt name localhost only
cp client.jks server2.jks
cat openssl.cnf.head altnames >openssl.cnf
openssl genrsa -out server2.key 2048
openssl req -new -key server2.key -out server2.csr -config openssl.cnf
openssl x509 -req -in server2.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out server2.crt -days 1095 -sha256 -extensions req_ext -extfile openssl.cnf
openssl pkcs12 -export -inkey server2.key -in server2.crt -certfile rootCA.pem -out server2.p12
keytool -importkeystore -srckeystore server2.p12 -srcstorepass password -destkeystore server2.jks -deststorepass password -srcstoretype pkcs12 -deststoretype jks

# clean the temporary files
rm openssl.cnf rootCA.key rootCA.pem rootCA.srl server.crt server.csr server.key server.p12 server2.crt server2.csr server2.key server2.p12
