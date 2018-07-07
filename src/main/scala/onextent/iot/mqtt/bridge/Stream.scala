package onextent.iot.mqtt.bridge

import scala.concurrent.ExecutionContext.Implicits.global
import akka.NotUsed
import akka.stream.ThrottleMode
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, RestartSource, Source}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.Conf._
import onextent.iot.mqtt.bridge.models.SayHello

import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.{Failure, Success}

/**
  * 2 streams - a cmd and a mqtt listener.
  * it was a single sink with MergeHub but was failing silently, RestartSource wasn't seeing a future to watch...
  * todo: why isn't it enough to have RestartSource, do I really need the onComplete??
  */
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

    val mqttStream = RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        MqttSource.atMostOnce(srcSettings, 8)
      }
      .runWith(MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    mqttStream onComplete {
      case Success(_) =>
        logger.warn("success. but hello stream should not end!")
        actorSystem.terminate()
      case Failure(e) =>
        logger.error(s"failure. hello stream should not end! $e", e)
        actorSystem.terminate()
    }

    val cmdStream = RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        Source.fromGraph(new HelloSource()).via(throttlingFlow)
      }
      .map(helloMqttMessage())
      .runWith(MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    cmdStream onComplete {
      case Success(_) =>
        logger.warn("success. but hello stream should not end!")
        actorSystem.terminate()
      case Failure(e) =>
        logger.error(s"failure. hello stream should not end! $e", e)
        actorSystem.terminate()
    }

  }

}
