# Etapa 1: compila o projeto (tem Maven e JDK)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q package -DskipTests

# Etapa 2: imagem final, só com o Java para rodar (menor)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/carrinho-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]