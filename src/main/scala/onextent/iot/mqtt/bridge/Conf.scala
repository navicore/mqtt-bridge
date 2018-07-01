package onextent.iot.mqtt.bridge

import akka.actor.ActorSystem
import akka.stream._
import com.typesafe.config.{Config, ConfigFactory}
import com.typesafe.scalalogging.LazyLogging

object Conf extends Conf with LazyLogging {

  implicit val actorSystem: ActorSystem = ActorSystem("MqttKafka")

  val decider: Supervision.Decider = { e: Throwable =>
    logger.error(s"decider can not decide: $e - restarting...", e)
    Supervision.Restart
  }

  implicit val materializer: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem).withSupervisionStrategy(decider))
}

trait Conf {

  val conf: Config = ConfigFactory.load()

  val mqttSubscribeClientId: String = conf.getString("mqtt.subscribe.clientId")
  val mqttSubscribeUrl: String = conf.getString("mqtt.subscribe.url")
  val mqttSubscribeUser: String = conf.getString("mqtt.subscribe.user")
  val mqttSubscribePwd: String = conf.getString("mqtt.subscribe.pwd")
  val mqttSubscribeTopic: String = conf.getString("mqtt.subscribe.topic")

}
