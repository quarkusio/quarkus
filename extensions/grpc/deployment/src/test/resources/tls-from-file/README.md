# Generating the certificates and keys

The ca is self-signed:
----------------------

```bash
openssl req -x509 -new -newkey rsa:2048 -nodes -keyout ca.key -out ca.pem \
  -config ca-openssl.cnf -days 3650 -extensions v3_req
```

When prompted for certificate information, everything is default.

Client is issued by CA:
-----------------------

```bash
openssl genrsa -out client.key.rsa 2048
openssl pkcs8 -topk8 -in client.key.rsa -out client.key -nocrypt
openssl req -new -key client.key -out client.csr
```

When prompted for certificate information, everything is default except the
common name which is set to `testclient`.

```bash
openssl x509 -req -CA ca.pem -CAkey ca.key -CAcreateserial -in client.csr \
  -out client.pem -days 3650
```

server is issued by CA with a special config for subject alternative names:
----------------------------------------------------------------------------

```bash
openssl genrsa -out server1.key.rsa 2048
openssl pkcs8 -topk8 -in server.key.rsa -out server.key -nocrypt
openssl req -new -key server.key -out server.csr -config server-openssl.cnf
```

When prompted for certificate information, everything is default except the
common name which is set to `localhost`.

```bash
openssl x509 -req -CA ca.pem -CAkey ca.key -CAcreateserial -in server.csr \
  -out server.pem -extfile server-openssl.cnf -days 3650
```

Cleanup
-------

```bash
rm *.rsa
rm *.csr
rm ca.srl
```