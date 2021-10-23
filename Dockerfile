FROM ghcr.io/graalvm/graalvm-ce:java11-21.3.0 as builder

ARG upx_compression
ARG print_reports
RUN gu install native-image
RUN curl -L https://www.scala-sbt.org/sbt-rpm.repo | tee /etc/yum.repos.d/sbt-rpm.repo && microdnf install sbt git xz

# BEGIN INSTALL PRE-REQUISITES FOR STATIC NATIVE IMAGES WITH GRAAL >= 20.2.0
# See:
#  - https://github.com/oracle/graal/issues/2824#issuecomment-685159371
#  - https://github.com/oracle/graal/blob/vm-20.3.0/substratevm/StaticImages.md
ARG TOOLCHAIN_DIR="/toolchain"

RUN mkdir ${TOOLCHAIN_DIR} && \
    curl -L -o musl.tar.gz http://musl.cc/x86_64-linux-musl-native.tgz && \
    tar -xvzf musl.tar.gz -C ${TOOLCHAIN_DIR} --strip-components 1 && \
    rm -f /musl.tar.gz

ENV PATH="$PATH:${TOOLCHAIN_DIR}/bin"
ENV CC="${TOOLCHAIN_DIR}/bin/gcc"

RUN curl -L -o zlib.tar.gz https://zlib.net/zlib-1.2.11.tar.gz && \
   mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && cd zlib && \
   ./configure --static --prefix=${TOOLCHAIN_DIR} && \
    make && make install && \
    cd / && rm -rf /zlib && rm -f /zlib.tar.gz
#END INSTALL PRE-REQUISITES FOR STATIC NATIVE IMAGES WITH GRAAL >= 20.2.0

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
