package onextent.iot.mqtt.bridge

import akka.{Done, NotUsed}
import akka.stream.ThrottleMode
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.Conf._
import onextent.iot.mqtt.bridge.models.SayHello

import scala.concurrent.Future
import scala.language.implicitConversions

object Stream extends LazyLogging {
  def throttlingFlow[T]: Flow[T, T, NotUsed] =
    Flow[T].throttle(
      elements = 1,
      per = intervalSeconds,
      maximumBurst = 0,
      mode = ThrottleMode.Shaping
    )

  def helloMqttMessage(): SayHello => MqttMessage =
    (h: SayHello) => {
      logger.debug(h.hello())
      MqttMessage(mqttPublishTopic,
                  ByteString(h.asJson()),
                  Some(MqttQoS.AtLeastOnce),
                  retained = true)
    }

  def apply(): Unit = {

    logger.info(s"stream starting...")

    val mqttSource: Source[MqttMessage, Future[Done]] =
      MqttSource.atMostOnce(srcSettings, 8)

    mqttSource
      .runWith(MqttSink(sinkSettings, MqttQoS.atLeastOnce))

  }

}
