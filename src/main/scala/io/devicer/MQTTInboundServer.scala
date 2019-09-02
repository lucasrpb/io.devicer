package io.devicer

import io.netty.handler.codec.mqtt.MqttQoS
import io.vertx.core.json._
import io.vertx.scala.core.Vertx
import io.vertx.scala.kafka.client.producer.{KafkaProducer, KafkaProducerRecord}
import io.vertx.scala.mqtt.messages.{MqttPublishMessage, MqttSubscribeMessage, MqttUnsubscribeMessage}
import io.vertx.scala.mqtt.{MqttEndpoint, MqttServer, MqttServerOptions}
import org.apache.kafka.clients.producer.ProducerConfig

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class MQTTInboundServer(val id: String)(implicit val ctx: ExecutionContext) {

  val vertx = Vertx.vertx()
  val options = MqttServerOptions()
    .setPort(8883)

  val mqttServer = MqttServer.create(vertx, options)

  val config = scala.collection.mutable.Map[String, String]()

  config += (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092")
  config += (ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG -> "org.apache.kafka.common.serialization.StringSerializer")
  config += (ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG -> "io.vertx.kafka.client.serialization.JsonObjectSerializer")
  config += (ProducerConfig.ACKS_CONFIG -> "1")

  // use producer for interacting with Apache Kafka
  val producer = KafkaProducer.create[String, JsonObject](vertx, config)

  def endpointHandler(endpoint: MqttEndpoint): Unit = {
    // shows main connect info
    println(s"MQTT client [${endpoint.clientIdentifier()}] request to connect, " +
      s"clean session = ${endpoint.isCleanSession()}")

    if (endpoint.auth() != null) {
      val auth = endpoint.auth().asJava
      println(s"[username = ${auth.getUsername}, password = ${auth.getPassword}]")
    }
    if (endpoint.will() != null) {
      val will = endpoint.will().asJava
      println(s"[will topic = ${will.getWillTopic} msg = ${new String(will.getWillMessageBytes)} QoS = ${will.getWillQos} " +
        s"isRetain = ${will.isWillRetain}]")
    }

    println(s"[keep alive timeout = ${endpoint.keepAliveTimeSeconds()}]")

    // accept connection from the remote client
    endpoint.accept(false)
    // handling disconnect message
    endpoint.disconnectHandler(_ => {
      println("Received disconnect from client")
    })

    endpoint.subscribeHandler((subcribe: MqttSubscribeMessage) => {
      var grantedQosLevels = ArrayBuffer.empty[MqttQoS]

      subcribe.topicSubscriptions().foreach(s => {
        println(s"Subscription for ${s.topicName()} with QoS ${s.qualityOfService()}")
        grantedQosLevels = grantedQosLevels :+ s.qualityOfService()
      })

      // ack the subscriptions request
      endpoint.subscribeAcknowledge(subcribe.messageId(), grantedQosLevels)
    })

    // handling requests for unsubscriptions
    endpoint.unsubscribeHandler((unsubscribe: MqttUnsubscribeMessage) => {

      unsubscribe.topics().foreach(t => {
        println(s"Unsubscription for ${t}")
      })

      // ack the subscriptions request
      endpoint.unsubscribeAcknowledge(unsubscribe.messageId())
    })

    // handling incoming published messages
    endpoint.publishHandler((message: MqttPublishMessage) => {

      println(s"Just received message [${message.payload().toString(java.nio.charset.Charset.defaultCharset())}] with QoS [${message.qosLevel()}]")

      if (message.qosLevel() == MqttQoS.AT_LEAST_ONCE) {

        val json = message.payload().toJsonObject()

        val record = KafkaProducerRecord.create[String, JsonObject]("devices", json.getString("id"), json)
        producer.writeFuture(record).onComplete {
          case Success(_) =>
            println(s"sucess writing to kafka topic!\n")
            endpoint.publishAcknowledge(message.messageId())
          case Failure(ex) =>
            println(s"error writing to kafka topic ${ex.getMessage}\n")
            endpoint.publishAcknowledge(message.messageId())
        }

      } else if (message.qosLevel() == MqttQoS.EXACTLY_ONCE) {
        endpoint.publishReceived(message.messageId())
      }

    }).publishReleaseHandler((mid: Int) => {
      endpoint.publishComplete(mid)
    })

  }

  mqttServer.endpointHandler(endpointHandler).listenFuture().onComplete{
    case Success(result) => {

      println(s"MQTT server is listening on port ${result.actualPort()}")
    }
    case Failure(cause) => {
      println(s"$cause")
    }
  }

}
