FROM amazoncorretto:21-alpine3.18
WORKDIR /app
COPY applications/app-service/build/libs/JavaHeadless.jar /app/app.jar

ENV JAVA_OPTS="-XX:+UseContainerSupport"
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]