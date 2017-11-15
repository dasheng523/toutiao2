FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/toutiao2.jar /toutiao2/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/toutiao2/app.jar"]
