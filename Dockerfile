FROM gradle:4.10.3-jdk8 as builder

COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
VOLUME /var/lib/docker/gradle/cache
RUN gradle build

FROM openjdk:8
COPY --from=builder /home/gradle/src/build/libs/ultimatelinz-1.0.jar /app/ultimatelinz-1.0.jar
WORKDIR /app
EXPOSE 90
CMD ["java", "-jar", "ultimatelinz-1.0.jar"]