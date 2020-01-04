FROM openjdk:8-jdk-slim as BUILD

# Get gradle distribution
COPY *.gradle gradle.* gradlew /src/
COPY gradle /src/gradle
WORKDIR /src
RUN ./gradlew --version

COPY . .
ENV MAIN_CLASS_NAME=main.MainKt
RUN ./gradlew --no-daemon jar

FROM openjdk:8
COPY --from=builder /src/build/libs/ultimatelinz-1.0.jar /app/ultimatelinz-1.0.jar
WORKDIR /app
EXPOSE 90
CMD ["java", "-jar", "ultimatelinz-1.0.jar"]