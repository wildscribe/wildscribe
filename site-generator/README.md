Wildscribe Site Generator
==================================

To generate a the model documentation for a single version execute the following command replacing the version numbers.

```
mvn clean verify -pl site-generator exec:java -Dversions.txt.dir=models/standalone/WildFly-17.0.0.Final.dmr -Dsite.url="" -Dserver.version=WildFly-17.0.0.Final
```
