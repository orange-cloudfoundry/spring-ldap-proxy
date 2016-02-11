FROM java:8
ADD spring-ldap-proxy-1.0.0.jar app.jar
RUN bash -c 'touch /app.jar'
ENV PORT 380
EXPOSE 380
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app.jar"]