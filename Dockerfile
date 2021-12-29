FROM clojure:alpine
VOLUME /data
LABEL description="code-maat docker image."

ARG dest=/usr/src/code-maat

RUN mkdir -p $dest
WORKDIR $dest
COPY project.clj $dest
RUN lein deps
COPY . $dest
RUN mv "$(lein uberjar | sed -n 's/^Created \(.*standalone\.jar\)/\1/p')" app-standalone.jar

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=85.0", "-jar", "app-standalone.jar"]
CMD []