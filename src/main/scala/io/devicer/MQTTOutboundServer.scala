package io.devicer

import io.vertx.core.json._
import io.vertx.scala.core.Vertx
import io.vertx.scala.kafka.client.consumer.{KafkaConsumer, KafkaConsumerRecord}
import org.apache.kafka.clients.consumer.ConsumerConfig

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MQTTOutboundServer(val id: String)(implicit val ec: ExecutionContext) {

  val config = scala.collection.mutable.Map[String, String]()

  config += (ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092")
  config += (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG -> "org.apache.kafka.common.serialization.StringDeserializer")
  config += (ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG -> "io.vertx.kafka.client.serialization.JsonObjectDeserializer")
  config += (ConsumerConfig.GROUP_ID_CONFIG -> s"outbound")
  config += (ConsumerConfig.AUTO_OFFSET_RESET_CONFIG -> "latest")
  config += (ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG -> "true")

  val vertx = Vertx.vertx()
  val consumer = KafkaConsumer.create[String, JsonObject](vertx, config)

  consumer.subscribeFuture("devices").onComplete {
    case Success(result) => {
      println(s"Consumer ${id} subscribed")
    }
    case Failure(cause) => println("Failure")
  }

  def handle(evt: KafkaConsumerRecord[String, JsonObject]): Unit = {
    val payload = evt.value()
    println(s"received payload ${payload}...\n")
  }

  consumer.handler(handle)
}
