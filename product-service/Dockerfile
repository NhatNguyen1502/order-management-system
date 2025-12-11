FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy the built jar (assumes you've run sbt assembly)
COPY product-service/target/scala-2.13/product-service-assembly-0.1.0-SNAPSHOT.jar /app/product-service.jar

EXPOSE 8083

CMD ["java", "-jar", "product-service.jar"]
