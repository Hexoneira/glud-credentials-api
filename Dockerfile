# Usamos la imagen oficial de Zulu OpenJDK 25 (solo JRE, que es súper ligera)
FROM azul-zulu:25-jre

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos el JAR compilado desde la carpeta target 
COPY target/*.jar app.jar

# Exponemos el puerto de Spring Boot (usualmente 8080)
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
