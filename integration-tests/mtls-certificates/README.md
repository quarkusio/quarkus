# mTLS Certificates

## Generate certificates steps

If prompted, trust certs and use password 'password'.

```shell
cat <<EOF > ./openssl.cnf
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = req_ext

[ dn ]
CN=client
OU=cert
O=quarkus
L=city
ST=state
C=AU

[ req_ext ]
subjectAltName = @altNames

[ altNames ]
otherName = 2.5.4.45;UTF8:redhat
email = certs@quarkus.io
dirName = test_dir
URI = https://www.quarkus.io/

[test_dir]
CN=client
OU=cert
O=quarkus
L=city
ST=state
C=AU
EOF

cat <<EOF > ./openssl-2.cnf
[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
req_extensions = req_ext

[ dn ]
CN=localhost
OU=quarkus
O=quarkus
L=city
ST=state
C=IE

[ req_ext ]
subjectAltName = @altNames

[ altNames ]
otherName = 2.5.4.45;UTF8:quarkus
email = certs-1@quarkus.io
dirName = test_dir
URI = https://www.vertx.io/

[test_dir]
CN=localhost
OU=quarkus
O=quarkus
L=city
ST=state
C=IE
EOF

openssl genrsa -out serverCA.key 2048
openssl req -x509 -new -nodes -key serverCA.key \
            -sha256 -days 9000 -out serverCA.pem \
            -extensions req_ext -config openssl.cnf
openssl pkcs12 -export -name server-cert \
               -in serverCA.pem -inkey serverCA.key \
               -out server-keystore.p12
keytool -import -alias localhost -storetype PKCS12 \
        -file serverCA.pem -keystore server-truststore.p12 -trustcacerts

openssl genrsa -out clientCA.key 2048
openssl req -x509 -new -nodes -key clientCA.key \
            -sha256 -days 9000 -out clientCA.pem \
            -extensions req_ext -config openssl.cnf
openssl pkcs12 -export -name client1-cert \
               -in clientCA.pem -inkey clientCA.key \
               -out client-keystore-1.p12
keytool -import -alias client1-cert -storetype PKCS12 \
        -file clientCA.pem -keystore client-truststore.p12 -trustcacerts
keytool -import -alias client1-cert -file clientCA.pem \
        -keystore server-truststore.p12 -trustcacerts

openssl genrsa -out client2CA.key 2048
openssl req -x509 -new -nodes -key client2CA.key \
            -sha256 -days 9000 -out client2CA.pem \
            -extensions req_ext -config openssl-2.cnf
openssl pkcs12 -export -name client2-cert \
               -in client2CA.pem -inkey client2CA.key \
               -out client-keystore-2.p12
keytool -import -alias client2-cert -file client2CA.pem \
        -keystore server-truststore.p12 -trustcacerts
keytool -import -alias client2-cert -file client2CA.pem \
        -keystore client-truststore.p12 -trustcacerts

keytool -import -alias server-cert -file serverCA.pem \
        -keystore client-truststore.p12 -trustcacerts
```