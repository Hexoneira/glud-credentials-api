<p align="center">
	<img src="https://capsule-render.vercel.app/api?type=waving&height=220&color=0:091e3a,50:132d52,100:1a3a52&text=GLUD%20CREDENTIALS%20API&fontColor=ffffff&fontSize=44&fontAlignY=40&desc=Hexoneira%20x%20GLUD%20%7C%20Secure%20Digital%20Credentials%20Platform&descAlignY=62&animation=fadeIn" alt="GLUD Credentials API Banner" />
</p>

<p align="center">
	<a href="https://www.oracle.com/java/" target="_blank"><img alt="Java" src="https://img.shields.io/badge/Java-25-0b132b?style=for-the-badge&logo=openjdk&logoColor=white"></a>
	<a href="https://spring.io/projects/spring-boot" target="_blank"><img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-4.0.4-1c2541?style=for-the-badge&logo=springboot&logoColor=white"></a>
	<a href="https://maven.apache.org/" target="_blank"><img alt="Maven" src="https://img.shields.io/badge/Build-Maven%20Wrapper-3a506b?style=for-the-badge&logo=apachemaven&logoColor=white"></a>
	<a href="https://www.postgresql.org/" target="_blank"><img alt="PostgreSQL" src="https://img.shields.io/badge/Database-PostgreSQL-5bc0be?style=for-the-badge&logo=postgresql&logoColor=0b132b"></a>
</p>

<p align="center">
	<img alt="Security" src="https://img.shields.io/badge/Security-JWT%20%7C%20Spring%20Security-0b132b?style=flat-square">
	<img alt="Architecture" src="https://img.shields.io/badge/Architecture-REST%20API-1c2541?style=flat-square">
	<img alt="Validation" src="https://img.shields.io/badge/Validation-Bean%20Validation-3a506b?style=flat-square">
	<img alt="ORM" src="https://img.shields.io/badge/Persistence-Spring%20Data%20JPA-5bc0be?style=flat-square&logoColor=0b132b">
</p>

## GLUD Credentials API

GLUD Credentials API es el backend oficial para la gestion de miembros y la emision de credenciales digitales dentro del ecosistema GLUD, desarrollado por Hexoneira.

El proyecto esta orientado a proveer una API robusta, segura y escalable para autenticacion, administracion de usuarios y validacion de credenciales.

## Alcance del proyecto

- Gestion de miembros con operaciones CRUD.
- Emision de credenciales digitales por usuario.
- Autenticacion y autorizacion segura con JWT.
- Validacion de credenciales mediante QR.
- Exposicion de endpoints REST para integracion con clientes web o moviles.

## Stack tecnologico

- Java 25
- Spring Boot 4.0.4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Bean Validation
- Lombok
- PostgreSQL
- Maven Wrapper

## Ejecucion del proyecto

### Requisitos

- JDK 25 instalado y configurado en PATH.
- Acceso a una instancia de PostgreSQL (recomendado: el contenedor de `docker-compose.yml`).

### Levantar PostgreSQL con Docker (recomendado)

```bash
docker compose up -d db
```

### Linux / macOS

Copia `.env.example` a `.env`, ajusta los valores y expone las variables al proceso. Maven no carga `.env` automáticamente, así que puedes exportarlas en tu shell:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

O de una sola línea:

```bash
env $(grep -v '^#' .env | xargs) ./mvnw spring-boot:run
```

> `RANDOM_SEED` no puede quedar vacío: es el secreto del servidor para derivar las seeds TOTP de invitados. Genera uno con `openssl rand -hex 32`.

### Windows (PowerShell o CMD)

```powershell
.\mvnw.cmd spring-boot:run
```

## Comandos de uso frecuente

```bash
# Compilar
./mvnw clean compile

# Ejecutar pruebas
./mvnw test

# Generar artefacto
./mvnw clean package
```

En Windows, reemplaza ./mvnw por .\mvnw.cmd.

## Configuracion base

El nombre de la aplicacion se encuentra en src/main/resources/application.yaml:

- spring.application.name=credentials

Para entornos productivos se recomienda externalizar credenciales y parametros sensibles mediante variables de entorno.

## Estructura principal

```text
src/
	main/
		java/org/glud/credentials/
			CredentialsApplication.java
		resources/
			application.yaml
	test/
		java/org/glud/credentials/
			CredentialsApplicationTests.java
```

## Organizacion

Proyecto desarrollado por Hexoneira para GLUD (Grupo Linux Universidad Distrital).
