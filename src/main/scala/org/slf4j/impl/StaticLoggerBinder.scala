package org.slf4j.impl

class StaticLoggerBinder extends app.OdinInterop

object StaticLoggerBinder extends StaticLoggerBinder {
  val REQUESTED_API_VERSION: String    = "1.7"
  def getSingleton: StaticLoggerBinder = this
}
