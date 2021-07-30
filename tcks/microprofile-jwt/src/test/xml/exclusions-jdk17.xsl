<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <!-- Retain DTD (yes, with http instead of https, otherwise testng will issue a warning) -->
    <xsl:output method="xml" doctype-system="http://testng.org/testng-1.0.dtd"/>

    <!-- Identity transform -->
    <xsl:template match="@* | node()">
        <xsl:copy>
            <xsl:apply-templates select="@* | node()"/>
        </xsl:copy>
    </xsl:template>

    <!-- Exclude TokenUtilsEncryptTest#testFailAlgorithm due to https://github.com/quarkusio/quarkus/issues/18372#issuecomment-877864662 -->
    <xsl:template match="class[@name = 'org.eclipse.microprofile.jwt.tck.util.TokenUtilsEncryptTest']">
        <xsl:copy>
            <xsl:copy-of select="@*"/>
            <xsl:copy-of select="node()"/>
            <methods><exclude name="testFailAlgorithm" /></methods>
        </xsl:copy>
    </xsl:template>

</xsl:stylesheet>
