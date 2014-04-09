Fabric8 BlueService
===================

This is a POC project for a major Fabric8 upgrade  

BlueService Goals
-----------------

Since project inception Fabric8 has come a long way. Originally designed to run on Karaf, it uses many of the container
provided services and is also limmited to those. New requirements have come up mainly in connection with the larger 
[RedHat xPaaS Strategy](http://www.redhat.com/about/news/archive/2013/9/welcome-to-the-world-of-xpaas)  

Specifically BlueService aims to 

* Provide a reliable/portable API for the Fabric8 system
* Verify that existing concepts can be ported to other containers
* Introduce missing concepts that are now required by xPaaS
* Provide API stability both for internal RedHat and external customers 

Supported Target Containers
---------------------------

The set of supported target containers includes but is not limited to 

* [Karaf](http://karaf.apache.org/)
* [Apache Tomcat](http://tomcat.apache.org/)
* [JBoss WildFly](http://www.wildfly.org/)

To achieve container portability, Fabric8 build on top of the [Fuse Portable Runtime](https://github.com/tdiesler/gravia/wiki)

Links
-----

* [Javadoc](http://174.129.32.31:8080/job/tdi-fabric8-poc/javadoc)
* [User Guide](../../wiki/User-Guide)
* [Developer Guide](../../wiki/Developer-Guide)
