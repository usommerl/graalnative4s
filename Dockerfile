FROM oracle/graalvm-ce:20.3.0-java11 as builder
RUN gu install native-image
RUN curl https://bintray.com/sbt/rpm/rpm | tee /etc/yum.repos.d/bintray-sbt-rpm.repo && \
    yum install -y sbt git

# BEGIN INSTALL PRE-REQUISITES FOR STATIC NATIVE IMAGES WITH GRAAL >= 20.2.0
# See:
#  - https://github.com/oracle/graal/issues/2824#issuecomment-685159371
#  - https://github.com/oracle/graal/blob/vm-20.3.0/substratevm/StaticImages.md
ARG RESULT_LIB="/staticlibs"

RUN mkdir ${RESULT_LIB} && \
    curl -L -o musl.tar.gz https://musl.libc.org/releases/musl-1.2.1.tar.gz && \
    mkdir musl && tar -xvzf musl.tar.gz -C musl --strip-components 1 && cd musl && \
    ./configure --disable-shared --prefix=${RESULT_LIB} && \
    make && make install && \
    cd / && rm -rf /muscl && rm -f /musl.tar.gz && \
    cp /usr/lib/gcc/x86_64-redhat-linux/4.8.2/libstdc++.a ${RESULT_LIB}/lib/

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

FROM scratch
COPY --from=builder /build/target/graalvm-native-image/graalnative4s /server
ENV PATH "/"
EXPOSE 8080
ENTRYPOINT [ "/server" ]
