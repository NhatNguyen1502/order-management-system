FROM openjdk:11-jre-slim

WORKDIR /app

# Copy the built jar (assumes you've run sbt assembly)
COPY order-service/target/scala-2.13/order-service-assembly-0.1.0.jar /app/order-service.jar

EXPOSE 8081 2551

CMD ["java", "-jar", "order-service.jar"]
