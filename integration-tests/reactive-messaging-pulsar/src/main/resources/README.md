# Generating the private and public keys

From https://pulsar.apache.org/docs/3.0.x/security-encryption/#get-started:

```bash
openssl ecparam -name secp521r1 -genkey -param_enc explicit -out test_ecdsa_privkey.pem
openssl ec -in test_ecdsa_privkey.pem -pubout -outform pem -out test_ecdsa_pubkey.pem
```
