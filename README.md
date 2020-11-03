# graalnative4s

![build](https://github.com/usommerl/graalnative4s/workflows/CI%2FCD/badge.svg)

Employ Scala for serverless workloads by using GraalVM native-image and Google Cloud Run.

## Rationale
I was wondering what is a good combination of purely functional Scala libraries that are easy to compile to a native-image. Well, and what do you do with a small image that starts up fast?

### Build
Use `sbt docker` to build the docker image locally. The Dockerfile provides or downloads all required GraalVM tooling. You don't need to install anything. Nonetheless, the image will be as minimal as possible by using a multi-stage build.

### Deploy
The [Github actions workflow](.github/workflows/ci_cd.yaml) will deploy the created image continuously. You could also use the Cloud Run button to allow others to deploy your application to their GCP accounts.

[![Run on Google Cloud](https://deploy.cloud.run/button.svg)](https://deploy.cloud.run)

### Use
The most recent version of this sample application is online here: [https://graalnative4s.usommerl.dev](https://graalnative4s.usommerl.dev)

### Contribution
Suggestions and contributions are welcome. Please feel free to use this as a template for your own projects.



