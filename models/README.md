Models Data Directory
=====================

This directory contains a representation of the model of various versions of the Wildfly, JBoss EAP and JBoss AS7 application
servers. These diffs are generated against the standalone-full-ha.xml profile, with the RTS, XTS and Agroal subsystems added to the profile
(note that some of the older models were generated against standalone-full.xml, and as such are missing some subsystems).

This data allows the Wildscribe pages to be generated offline, without a running server instance. It also means that at some
stage it should be possible to generate diff's of the models.

Add additional subsystems
=====================

$JBOSS_HOME/bin/jboss-cli.sh -c --commands=/extension=org.wildfly.extension.rts:add,/extension=org.jboss.as.xts:add,/extension=org.wildfly.extension.datasources-agroal:add,/extension=org.jboss.as.clustering.jgroups:add

Dumping models
=======================
To dump models:

 - Start the version of the server from which you wish to dump models.
 - Execute the model-dumper: 
    $ java -jar model-dumper/target/model-dumper.jar models/standalone/Wildfly-XYZ.Final.dmr
   
   Where XYZ is the version of the running server (for example 10.0.0-Final for Wildfly-10.0.0-Final). Output will be in models/standalone/ with the specified filename (Wildfly-XYZ.Final.dmr).

Note: Generation of the model may result in out-of-memory. Edit $WILDFLY_HOME/bin/standalone.conf, and increase the max heap (-Xmx) in JAVA_OPTS, and restart the server.
