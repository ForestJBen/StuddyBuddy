# StudyBuddy

StudyBuddy is a web application designed to help university students find compatible study partners, form study groups and organise study sessions.

## Continuous Integration

This project uses GitHub Actions to automatically build and test the Spring Boot application.

The workflow:

- Runs whenever code is pushed to any branch.
- Runs when a pull request targets the `main` branch.
- Uses Temurin JDK 17.
- Caches Maven dependencies.
- Builds and tests the project using `mvn -B clean verify --file pom.xml`.

The workflow configuration is located in [`.github/workflows/maven.yml`](.github/workflows/maven.yml).
