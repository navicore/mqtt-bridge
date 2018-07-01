package onextent.iot.mqtt.bridge

import akka.Done
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.Conf._

import scala.concurrent.Future
import scala.language.implicitConversions

object Stream extends LazyLogging {

  def apply(): Unit = {

    logger.info(s"stream starting...")

    val mqttSource: Source[MqttMessage, Future[Done]] =
      MqttSource.atMostOnce(srcSettings, 8)

    mqttSource
      .runWith(MqttSink(sinkSettings, MqttQoS.atLeastOnce))

  }

}
