# Certificate Generation

Run the following script using the SmallRye Certificate Generator:

```shell
> jbang ./GenerateCertificate.java
```

The script will update the `keystore.jks` and `truststore.jks` files in the current (`src/test/resources/certs`) directory.
The certificates are valid for 3 years.