FROM eclipse-temurin:11-jdk
WORKDIR /app
COPY src ./src/
COPY lib ./lib/
RUN javac -cp \"src:lib/*\" src/MobileApiServer.java
EXPOSE 8080
CMD [\"java\", \"-cp\", \"src:lib/*\", \"MobileApiServer\"]
