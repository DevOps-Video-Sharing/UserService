FROM openjdk:17-jdk

WORKDIR /app

# Copy the JAR file
COPY target/streaming-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy all PNG files from the specific directory to a directory in the Docker container
COPY src/main/java/com/programming/streaming/images/*.png /app/images/

# Expose port
EXPOSE 8080

# Command to run the application
CMD [ "java", "-jar", "/app/app.jar" ]
