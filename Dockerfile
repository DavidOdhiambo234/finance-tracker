FROM eclipse-temurin:11-jdk

WORKDIR /app

# Copy source and libraries
COPY src ./src/
COPY lib ./lib/
COPY config.properties ./config.properties

# Compile with correct classpath
RUN javac -cp "src:lib/*" src/MobileApiServer.java

# Expose port
EXPOSE 8080

# Run the application
CMD ["java", "-cp", "src:lib/*", "MobileApiServer"]
