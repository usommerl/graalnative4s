FROM oracle/graalvm-ce:20.1.0-java11 as builder
ARG upx_compression
RUN gu install native-image
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo && \
    yum install -y sbt git xz
COPY . /build
WORKDIR /build
RUN curl -L -o musl.tar.gz \
    https://github.com/gradinac/musl-bundle-example/releases/download/v1.0/musl.tar.gz && \
    tar -xvzf musl.tar.gz
RUN sbt graalvm-native-image:packageBin

RUN if [ -n "${upx_compression}" ]; then \
      curl -L -o upx-3.96-amd64_linux.tar.xz https://github.com/upx/upx/releases/download/v3.96/upx-3.96-amd64_linux.tar.xz && \
      tar -xvf upx-3.96-amd64_linux.tar.xz && \
      upx-3.96-amd64_linux/upx "$upx_compression" /build/target/graalvm-native-image/graalnative4s; \
    fi

FROM scratch
COPY --from=builder /build/target/graalvm-native-image/graalnative4s /server
ENV PATH "/"
EXPOSE 8080
ENTRYPOINT [ "/server" ]
