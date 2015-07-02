FROM java:8

RUN apt-get update

ADD target/greed-0.1.0-SNAPSHOT-standalone.jar /srv/greed.jar

CMD ["java", "-jar", "/srv/greed.jar"]

