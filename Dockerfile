
# ETAPA 1: COMPILAR EL MICROSERVICIO
FROM maven:3.9-eclipse-temurin-21 AS buildstage

WORKDIR /app

COPY pom.xml .

COPY src ./src

RUN mvn -B clean package

# ETAPA 2: EJECUTAR EL MICROSERVICIO
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=buildstage \
    /app/target/ms-administracion-archivos-1.0.0.jar \
    /app/app.jar

# Carpeta donde montaremos AWS EFS
RUN mkdir -p /app/efs

# Puerto del microservicio
EXPOSE 8080

# Iniciar Spring Boot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]