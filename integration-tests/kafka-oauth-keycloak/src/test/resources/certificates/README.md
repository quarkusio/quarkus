# Generating the certificates and keystore

## Creating a self-signed CA certificate and truststore

```bash
./gen-ca.sh
```

This creates `crt.ca` and adds the certificate to the keystore `ca-truststore.p12`.

## Creating a server certificate and add it to keystore

```bash
./gen-keycloak-certs.sh
```

This creates server certificate for Keycloak, signs it and adds it to keystore `keycloak.server.keystore.p12`.

## Cleanup

```bash
rm ca.srl
rm ca.crt
rm ca.key
rm cert-file
rm cert-signed
```