FROM openjdk:8-jdk-slim as builder

# Get gradle distribution
COPY *.gradle gradle.* gradlew /src/
COPY gradle /src/gradle
WORKDIR /src
RUN chmod 777 gradlew
RUN ./gradlew --version

COPY . .
ENV MAIN_CLASS_NAME=main.MainKt
RUN chmod 777 gradlew
RUN ./gradlew --no-daemon jar

FROM openjdk:8
COPY --from=builder /src/build/libs/ultimatelinz-1.0.jar /app/ultimatelinz-1.0.jar
WORKDIR /app
EXPOSE 90
CMD ["java", "-jar", "ultimatelinz-1.0.jar"]