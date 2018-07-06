package onextent.iot.mqtt.bridge

import scala.concurrent.ExecutionContext.Implicits.global
import akka.Done
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.Source
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.Conf._

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object Stream extends LazyLogging {

  def apply(): Unit = {

    logger.info(s"stream starting...")

    val mqttSource: Source[MqttMessage, Future[Done]] =
      MqttSource.atMostOnce(srcSettings, 8)

    val r = mqttSource
      .runWith(MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    r onComplete {
      case Success(_) =>
        logger.warn("success. but stream should not end!")
        actorSystem.terminate()
      case Failure(e) =>
        logger.error(s"failure. stream should not end! $e", e)
        actorSystem.terminate()
    }
  }

}
