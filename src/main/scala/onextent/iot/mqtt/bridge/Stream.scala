package onextent.iot.mqtt.bridge

import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, Keep, MergeHub, RestartSource, Sink, Source}
import akka.stream.{Materializer, ThrottleMode}
import akka.util.ByteString
import akka.{Done, NotUsed}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.Conf._
import onextent.iot.mqtt.bridge.models.SayHello

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions
import scala.util.{Failure, Success}

object Stream extends LazyLogging {

  def createToConsumer(consumer: Sink[MqttMessage, Future[Done]])(
      implicit s: ActorSystem,
      m: Materializer): (Sink[MqttMessage, NotUsed], Future[Done]) = {
    val r: (Sink[MqttMessage, NotUsed], Future[Done]) = MergeHub
      .source[MqttMessage](perProducerBufferSize = 16)
      .toMat(consumer)(Keep.both)
      .run()

    r
  }

  def throttlingFlow[T]: Flow[T, T, NotUsed] =
    Flow[T].throttle(
      elements = 1,
      per = intervalSeconds,
      maximumBurst = 0,
      mode = ThrottleMode.Shaping
    )

  def helloMqttMessage(): SayHello => MqttMessage = {
    val topic = s"$mqttPublishTopicPrefix$mqttPublishTopicSuffix"
    h: SayHello =>
      {
        logger.debug(s"topic: $topic msg: ${h.hello()}")
        MqttMessage(topic,
                    ByteString(h.asJson()),
                    Some(MqttQoS.AtLeastOnce),
                    retained = true)
      }
  }

  def handleTerminate(result: Future[Done]): Unit = {
    result onComplete {
      case Success(_) =>
        logger.warn("success. but stream should not end!")
        actorSystem.terminate()
      case Failure(e) =>
        logger.error(s"failure. stream should not end! $e", e)
        actorSystem.terminate()
    }
  }

  def apply(): Unit = {

    logger.info(s"stream starting...")

    val toConsumer = createToConsumer(
      MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        MqttSource.atMostOnce(srcSettings, 8)
      }
      .runWith(toConsumer._1)

    //handleTerminate(mqttStream)

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        Source.fromGraph(new HelloSource()).via(throttlingFlow)
      }
      .map(helloMqttMessage())
      .runWith(toConsumer._1)

    handleTerminate(toConsumer._2)

  }

}
