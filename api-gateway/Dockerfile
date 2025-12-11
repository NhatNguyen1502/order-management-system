FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar (assumes you've run sbt assembly)
COPY api-gateway/target/scala-2.13/api-gateway-assembly-0.1.0-SNAPSHOT.jar /app/api-gateway.jar

EXPOSE 8080

CMD ["java", "-jar", "api-gateway.jar"]
