Wildscribe Documentation Generator
==================================

This project is a documentation generator for Wildfly/JBoss EAP. Basically takes the self describing management model
and turns it into HTML. It consists of two parts, the model dumper and the site generator.

See models/README.md for details on dumping models.

To generate the site:

Add any new model versions to models/standalone/versions.txt

Then run (for example):

$ java -Durl=http://wildscribe.github.io -jar site-generator/target/site-generator.jar models/standalone/ ../wildscribe.github.io/
