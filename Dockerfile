# FROM openjdk:17-jdk

# WORKDIR /app

# COPY target/streaming-0.0.1-SNAPSHOT.jar /app.jar
# EXPOSE 8080


# CMD [ "java", "-jar", "/app.jar" ]


FROM openjdk:17-jdk

WORKDIR /app

# Copy the JAR file
COPY target/streaming-0.0.1-SNAPSHOT.jar /app/app.jar

# Copy all image files to a directory in the Docker container
COPY *.png /app/images/

# Expose port
EXPOSE 8080

# Command to run the application
CMD [ "java", "-jar", "/app/app.jar" ]
