FROM ghcr.io/graalvm/native-image-community:23.0.2-muslib-ol9 as builder

ARG upx_compression
ARG print_reports
RUN curl -L https://www.scala-sbt.org/sbt-rpm.repo | tee /etc/yum.repos.d/sbt-rpm.repo && microdnf install sbt git xz

COPY . /build
WORKDIR /build
RUN sbt graalvm-native-image:packageBin

RUN if [ -n "${print_reports}" ]; then \
        for f in target/graalvm-native-image/reports/*.csv; do \
          printf '#%.0s' {1..80}; \
          printf "\n$f\n\n"; \
          cat $f; \
        done; \
    fi

RUN if [ -n "${upx_compression}" ]; then \
      curl -L -o upx-3.96-amd64_linux.tar.xz https://github.com/upx/upx/releases/download/v3.96/upx-3.96-amd64_linux.tar.xz && \
      tar -xvf upx-3.96-amd64_linux.tar.xz && \
      upx-3.96-amd64_linux/upx "$upx_compression" /build/target/graalvm-native-image/graalnative4s; \
    fi

RUN echo 'unprivileged:x:65534:65534:unprivileged:/:' > /etc/passwd.minimal

FROM scratch
COPY --from=builder /build/target/graalvm-native-image/graalnative4s /server
COPY --from=builder /etc/passwd.minimal /etc/passwd
USER unprivileged
ENV PATH "/"
EXPOSE 8080
ENTRYPOINT [ "/server" ]
