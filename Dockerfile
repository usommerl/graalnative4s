FROM ghcr.io/graalvm/graalvm-ce:java11-21.1.0 as builder

ARG upx_compression
RUN gu install native-image
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo && microdnf install sbt git xz

# BEGIN INSTALL PRE-REQUISITES FOR STATIC NATIVE IMAGES WITH GRAAL >= 20.2.0
# See:
#  - https://github.com/oracle/graal/issues/2824#issuecomment-685159371
#  - https://github.com/oracle/graal/blob/vm-20.3.0/substratevm/StaticImages.md
ARG RESULT_LIB="/staticlibs"

RUN mkdir ${RESULT_LIB} && \
    curl -L -o musl.tar.gz https://musl.libc.org/releases/musl-1.2.2.tar.gz && \
    mkdir musl && tar -xvzf musl.tar.gz -C musl --strip-components 1 && cd musl && \
    ./configure --disable-shared --prefix=${RESULT_LIB} && \
    make && make install && \
    cd / && rm -rf /muscl && rm -f /musl.tar.gz && \
    cp /usr/lib/gcc/x86_64-redhat-linux/8/libstdc++.a ${RESULT_LIB}/lib/

ENV PATH="$PATH:${RESULT_LIB}/bin"
ENV CC="musl-gcc"

RUN curl -L -o zlib.tar.gz https://zlib.net/zlib-1.2.11.tar.gz && \
   mkdir zlib && tar -xvzf zlib.tar.gz -C zlib --strip-components 1 && cd zlib && \
   ./configure --static --prefix=${RESULT_LIB} && \
    make && make install && \
    cd / && rm -rf /zlib && rm -f /zlib.tar.gz
#END INSTALL PRE-REQUISITES FOR STATIC NATIVE IMAGES WITH GRAAL >= 20.2.0

COPY . /build
WORKDIR /build
RUN sbt graalvm-native-image:packageBin
RUN for f in target/graalvm-native-image/reports/*.txt; do printf '#%.0s' {1..80}; printf "\n$f\n\n"; cat $f; done

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
