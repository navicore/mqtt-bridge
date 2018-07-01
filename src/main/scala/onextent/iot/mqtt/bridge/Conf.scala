package onextent.iot.mqtt.bridge

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream._
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttQoS, MqttSourceSettings}
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

object Conf extends Conf with LazyLogging {

  implicit val actorSystem: ActorSystem = ActorSystem("MqttKafka")

  val decider: Supervision.Decider = { e: Throwable =>
    logger.error(s"decider can not decide: $e - restarting...", e)
    Supervision.Restart
  }

  implicit val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))
}

trait Conf extends LazyLogging {

  val conf: Config = ConfigFactory.load()

  val host: String = conf.getString("mqtt.subscribe.host")
  val proto: String = conf.getString("mqtt.subscribe.proto")
  val port: Int = conf.getInt("mqtt.subscribe.port")
  val mqttSubscribeUrl: String = s"$proto://$host:$port"
  val mqttSubscribeClientId: String = conf.getString("mqtt.subscribe.clientId")
  val mqttSubscribeUser: String = conf.getString("mqtt.subscribe.user")
  val mqttSubscribePwd: String = conf.getString("mqtt.subscribe.pwd")
  val mqttSubscribeTopic: String = conf.getString("mqtt.subscribe.topic")

  val mqttPublishClientId: String = conf.getString("mqtt.publish.clientId")
  val mqttPublishUrl: String = conf.getString("mqtt.publish.url")
  val mqttPublishUser: String = conf.getString("mqtt.publish.user")
  val mqttPublishPwd: String = conf.getString("mqtt.publish.pwd")
  val mqttPublishTopic: String = conf.getString("mqtt.publish.topic")

  logger.info(s"subscribing to $mqttSubscribeUrl")
  val srcSettings = MqttSourceSettings(
    MqttConnectionSettings(
      mqttSubscribeUrl,
      mqttSubscribeClientId,
      new MemoryPersistence
    ).withAuth(mqttSubscribeUser, mqttSubscribePwd)
      .withAutomaticReconnect(true)
      .withKeepAliveInterval(15, SECONDS),
    Map(mqttSubscribeTopic -> MqttQoS.AtLeastOnce)
  )

  logger.info(s"publishing to $mqttPublishUrl")
  val pubSettings: MqttConnectionSettings =
    MqttConnectionSettings(
      mqttPublishUrl,
      mqttPublishClientId,
      new MemoryPersistence
    ).withAuth(mqttPublishUser, mqttPublishPwd)

  val sinkSettings: MqttConnectionSettings =
    pubSettings.withClientId(clientId = mqttPublishClientId)
}
