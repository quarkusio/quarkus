#!/bin/sh

deleteFile() {
    file=${1}
    if [ -e ${file} ]; then
        rm ${file}
    fi

}
dir=$(dirname ${0})
pushd ${dir}

# Delete the current files
deleteFile server-keystore.jks
deleteFile server.cer
deleteFile client-keystore.jks
deleteFile client.cer

dname="CN=localhost, OU=Server Unit, O=Red Hat, L=Raleigh, S=NC, C=US"
# Create server keystore - file server-keystore.jks
keytool -genkey -v -alias server -keystore server-keystore.jks -keyalg RSA -validity 3650 -keypass testpassword -storepass testpassword -dname "${dname}"

# Export Server's Public Key - file server.cer
keytool -export -keystore server-keystore.jks -alias server -file server.cer -keypass testpassword -storepass testpassword

# Export Client Key Store - file client-keystore.jsk
keytool -genkey -v -alias client -keystore client-keystore.jks -keyalg RSA -validity 3650 -keypass testpassword -storepass testpassword -dname "${dname}"

# Exporting Client's Public Key - file client.cer
keytool -export -keystore client-keystore.jks -alias client -file client.cer -keypass testpassword -storepass testpassword

# Importing Client's Public key into server's truststore
keytool -import -v -trustcacerts -alias client -file client.cer -keystore server-keystore.jks -keypass testpassword -storepass testpassword -noprompt

# Importing Server's Public key into client's truststore
keytool -import -v -trustcacerts -alias server -file server.cer -keystore client-keystore.jks -keypass testpassword -storepass testpassword -noprompt

popd
