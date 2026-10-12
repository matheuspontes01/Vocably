# Etapa 1: compila o projeto com Maven (a imagem final não precisa do Maven nem do código-fonte).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Baixar as dependências antes de copiar o código faz o Docker reaproveitar essa camada
# enquanto o pom.xml não mudar: os próximos builds ficam bem mais rápidos.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Os testes rodam fora do Docker (./mvnw test) e no CI: eles próprios precisam do Docker (Testcontainers).
RUN mvn -B -q package -DskipTests

# Etapa 2: só o Java (JRE) e o .jar gerado.
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
