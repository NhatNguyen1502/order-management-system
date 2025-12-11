FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar (assumes you've run sbt assembly)
COPY inventory-service/target/scala-2.13/inventory-service-assembly-0.1.0-SNAPSHOT.jar /app/inventory-service.jar

EXPOSE 8082 2552

CMD ["java", "-jar", "inventory-service.jar"]
