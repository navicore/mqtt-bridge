package onextent.iot.mqtt.bridge

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.alpakka.mqtt._
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, MergeHub, RestartSource, RunnableGraph, Sink, Source}
import akka.stream.{Materializer, ThrottleMode}
import akka.util.ByteString
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.Conf._
import onextent.iot.mqtt.bridge.models.SayHello

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.implicitConversions

object Stream extends LazyLogging {

  def createToConsumer(consumer: Sink[MqttMessage, Future[Done]])(
      implicit s: ActorSystem,
      m: Materializer): Sink[MqttMessage, NotUsed] = {
    val runnableGraph: RunnableGraph[Sink[MqttMessage, NotUsed]] =
      MergeHub
        .source[MqttMessage](perProducerBufferSize = 16)
        .to(consumer)
    runnableGraph.run()
  }

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

    val toConsumer: Sink[MqttMessage, NotUsed] = createToConsumer(
      MqttSink(sinkSettings, MqttQoS.atLeastOnce))

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        MqttSource.atMostOnce(srcSettings, 8)
      }
      .to(toConsumer)
      .run()

    RestartSource
      .withBackoff(minBackoff = 1 second,
                   maxBackoff = 10 seconds,
                   randomFactor = 0.2) { () =>
        Source.fromGraph(new HelloSource()).via(throttlingFlow)
      }
      .map(helloMqttMessage())
      .to(toConsumer)
      .run()

  }

}
