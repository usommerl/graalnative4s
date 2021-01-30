# graalnative4s

![build](https://img.shields.io/github/workflow/status/usommerl/graalnative4s/CI?style=for-the-badge)
[![codecov](https://img.shields.io/codecov/c/github/usommerl/graalnative4s?style=for-the-badge)](https://codecov.io/gh/usommerl/graalnative4s)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=for-the-badge)](https://scala-steward.org)

This is a showcase for a combination of purely functional Scala libraries that can be used with GraalVM `native-image` without much effort. It employs [http4s][http4s] for general server functionality, [circe][circe] for JSON processing, [pureconfig] to load runtime configuration, [tapir][tapir] to describe HTTP endpoints and [odin][odin] for logging. Applications that were built with `native-image` have beneficial properties such as a lower memory footprint and fast startup. This makes them suitable for serverless applications.

### Build

**Please note that it is currently impossible to build a docker image** because Oracle removed all GraalVM images from Docker Hub. They are now using [ghcr.io to publish GraalVM images but did not upload an image for v20.1.0](https://github.com/orgs/graalvm/packages/container/graalvm-ce/versions). I have made several attempts to upgrade to newer GraalVM versions but was not successful so far (See: #16, #51).

~~Use `sbt docker` to build a docker image with the native image binary. You don't need to install anything besides `docker` and `sbt`, the build process downloads all required GraalVM tooling. The [created image][image] will be as minimal as possible by using a multi-stage build.~~

~~You can create an even smaller image by utilizing UPX compression. Use the `UPX_COMPRESSION` environment variable at build time to specify the compression level.
Please note that while this reduces the size of the image significantly it also [has an impact on startup performance and memory consumption.](./benchmark/upx.md)~~

~~Example: `export UPX_COMPRESSION='--best'; sbt docker`~~

### Deploy
This repository contains a [workflow][workflow] that will deploy the created image to Google Cloud Run. You could also use the button below to deploy it to your own GCP account.

[![Run on Google Cloud](https://deploy.cloud.run/button.svg)](https://deploy.cloud.run)

### Try
The most recent version of this small example is online here: [https://graalnative4s.usommerl.dev](https://graalnative4s.usommerl.dev)

### Acknowledgements & Participation
I have taken a lot of inspiration and knowledge from [this blog post by James Ward][inspiration]. You should check out his [hello-uzhttp][uzhttp] example. Another project that helped me to connect the dots regarding `native-image` configuration was [vasilmkd/docker-stats-monitor][docker-stats-monitor]. Suggestions and contributions to this repository are welcome!

[http4s]: https://github.com/http4s/http4s
[circe]: https://github.com/circe/circe
[tapir]: https://github.com/softwaremill/tapir
[odin]: https://github.com/valskalla/odin
[pureconfig]: https://github.com/pureconfig/pureconfig

[image]: https://github.com/users/usommerl/packages/container/package/graalnative4s
[workflow]: .github/workflows/ci_cd.yaml
[inspiration]: https://jamesward.com/2020/05/07/graalvm-native-image-tips-tricks/
[uzhttp]: https://github.com/jamesward/hello-uzhttp
[docker-stats-monitor]: https://github.com/vasilmkd/docker-stats-monitor

