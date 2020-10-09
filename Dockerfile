FROM hseeberger/scala-sbt:graalvm-ce-20.0.0-java11_1.4.0_2.13.3 as builder
RUN gu install native-image
COPY . /build
WORKDIR /build
RUN curl -L -o musl.tar.gz \
    https://github.com/gradinac/musl-bundle-example/releases/download/v1.0/musl.tar.gz && \
    tar -xvzf musl.tar.gz
RUN sbt graalvm-native-image:packageBin

FROM scratch
COPY --from=builder /build/target/graalvm-native-image/scala-graalvm-webservice /server
ENV PATH "/"
EXPOSE 8080
ENTRYPOINT [ "/server" ]
