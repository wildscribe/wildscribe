Models Data Directory
=====================

This directory contains a representation of the model of various versions of the Wildfly, JBoss EAP and JBoss AS7 application
servers. These diffs are generated against the standalone-full-ha.xml profile, with the XTS subsystem added to the profile
(note that some of the older models were generated against standalone-full.xml, and as such are missing some subsystems).

This data allows the Wildscribe pages to be generated offline, without a running server instance. It also means that at some
stage it should be possible to generate diff's of the models.
