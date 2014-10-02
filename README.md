ISP-ng
======

##Toolstack for next-gen ISPs

Currently, this daemon aims to ease OpenVPN management (through OpenVPN's management channel), handling user registration, authentication
and the assigment of IP addresses from a pool (be it fixed or dynamic). It exposes a REST API, which allows you to add 
IP address pools, manage active connections etc.

**Build instructions**

1. Install JDK 8
2. Install Maven (apt-get install maven)
3. mvn clean install

**Run instructions**

1. Install JRE/JDK 8
2.  Install jsvc (commons-daemon) and zookeeper
3.  Configure the OpenVPN MGMT channel and database in config.properties (see config.properties.default)
4. jsvc -cp ISP-NG-VPN-jar-with-dependencies.jar -home /usr/lib/jvm/java-8-oracle/ -user ispng -pidfile /var/run/ispng.pid  be.neutrinet.ispng.VPN

This will auto-create the database tables and run Jetty embedded on https://[hostname]:[port].
However it is also possible to use another webserver to serve the static files (CORS headers are set).
