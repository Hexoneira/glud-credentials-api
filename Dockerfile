# Imagen ligera solo con JRE para ejecutar la app
FROM azul-zulu:25-jre

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el JAR ya generado por CI
COPY target/*.jar app.jar

# Exponemos el puerto de Spring Boot (usualmente 8080)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
