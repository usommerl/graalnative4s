package server

case class Configuration(port: Int, apiDocs: ApiDocsConfiguration)
case class ApiDocsConfiguration(serverUrl: String)
