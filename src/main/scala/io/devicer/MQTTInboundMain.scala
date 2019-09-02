package io.devicer

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global

object MQTTInboundMain {

  def main(args: Array[String]): Unit = {

    new MQTTInboundServer(UUID.randomUUID.toString)

  }

}
