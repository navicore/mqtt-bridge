package onextent.iot.mqtt.bridge

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.models.SayHello
import onextent.iot.mqtt.bridge.Conf._

class HelloSource(implicit system: ActorSystem)
    extends GraphStage[SourceShape[SayHello]] with LazyLogging {

  val out: Outlet[SayHello] = Outlet("SayHelloSource")

  override val shape: SourceShape[SayHello] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            push(out, SayHello(myName))
          }
        }
      )
    }

}
