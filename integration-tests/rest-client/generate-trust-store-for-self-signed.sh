#!/usr/bin/env bash

##########################################################################################################################################
# Script that generates the trustStore containing the self signed certificate used in 'io.quarkus.it.rest.client.selfsigned.ExternalSelfSignedResource' #
##########################################################################################################################################


echo -n | openssl s_client -connect self-signed.badssl.com:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > self-signed.cert
keytool -importcert -file self-signed.cert -alias self-signed -keystore self-signed -storepass changeit -noprompt
rm self-signed.cert
