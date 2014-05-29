ISP-ng
======

Toolstack for next-gen ISPs

**Build instructions**

1. Install JDK 8
2. Install Maven (apt-get install maven2)
3. mvn clean install

**Run instructions**

1. Install JRE/JDK 8
2. Configure the OpenVPN MGMT channel and database in config.properties (see config.properties.default)
3. java -jar target/ISP-NG-VPN-jar-with-dependencies.jar

This will auto-create the database tables and run Jetty embedded on 8080.
However it is also possible to use another webserver to serve the static files (CORS headers are set).
