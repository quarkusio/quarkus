#!/usr/bin/env bash

##########################################################################################################################################
# Script that generates the trustStore containing the self signed certificate used in 'io.quarkus.it.rest.client.selfsigned.ExternalSelfSignedResource' #
##########################################################################################################################################


echo -n | openssl s_client -connect self-signed.badssl.com:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > self-signed.cert
keytool -importcert -file self-signed.cert -alias self-signed -keystore self-signed -storepass changeit -noprompt
rm self-signed.cert


echo -n | openssl s_client -connect wrong.host.badssl.com:443 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > wrong-host.cert
keytool -importcert -file wrong-host.cert -alias wrong-host -keystore wrong-host -storepass changeit -noprompt
rm wrong-host.cert
