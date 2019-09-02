package io.devicer

import java.util.UUID

import scala.concurrent.ExecutionContext.Implicits.global

object MQTTOutboundMain {

  def main(args: Array[String]): Unit = {

    new MQTTOutboundServer(UUID.randomUUID.toString)

  }

}
