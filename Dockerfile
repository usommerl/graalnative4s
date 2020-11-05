FROM oracle/graalvm-ce:20.1.0-java11 as builder
RUN gu install native-image
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo && \
    yum install -y sbt git
COPY . /build
WORKDIR /build
RUN curl -L -o musl.tar.gz \
    https://github.com/gradinac/musl-bundle-example/releases/download/v1.0/musl.tar.gz && \
    tar -xvzf musl.tar.gz
RUN sbt clean compile
RUN sbt graalvm-native-image:packageBin

FROM scratch
COPY --from=builder /build/target/graalvm-native-image/graalnative4s /server
ENV PATH "/"
EXPOSE 8080
ENTRYPOINT [ "/server" ]
