FROM maven:3.8.4-openjdk-17 AS builder
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true

FROM maven:3.8.4-openjdk-17
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8088
ENTRYPOINT ["java","-jar","app.jar"]
