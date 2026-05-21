# Etapa de compilación: construimos el JAR dentro de la imagen
FROM maven:3.9.9-eclipse-temurin-25 AS builder

# Directorio de trabajo para la compilación
WORKDIR /build

# Copiamos los archivos necesarios para compilar con Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
COPY src src

# Generamos el JAR de la aplicación
RUN mvn -B -DskipTests package

# Etapa final: imagen ligera solo con JRE para ejecutar la app
FROM azul-zulu:25-jre

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el JAR generado en la etapa anterior
COPY --from=builder /build/target/*.jar app.jar

# Exponemos el puerto de Spring Boot (usualmente 8080)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
