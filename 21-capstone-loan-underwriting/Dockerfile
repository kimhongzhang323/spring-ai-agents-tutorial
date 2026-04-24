FROM eclipse-temurin:21-jre
WORKDIR /app
COPY 21-capstone-loan-underwriting/target/21-capstone-loan-underwriting-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
