File buildLog = new File( basedir, 'build.log' )

// Test should fail - one dependency is older than reference (allowNewer), one is mismatched (default)
assert buildLog.text.contains( 'Dependency version alignment check failed' )

// byte-buddy 1.13.0 is older than reference 1.14.7 with allowNewer=true: should fail
assert buildLog.text.contains( 'net.bytebuddy:byte-buddy' )
assert buildLog.text.contains( 'which is older than' )

// antlr4 4.10.1 != reference 4.13.0 with default exact-match: should fail
assert buildLog.text.contains( 'org.antlr:antlr4' )
assert buildLog.text.contains( 'but reference artifact expects' )

// jakarta.persistence-api 3.2.0 > reference 3.1.0 with allowNewer=true: should pass
assert !buildLog.text.contains( 'jakarta.persistence:jakarta.persistence-api' )
