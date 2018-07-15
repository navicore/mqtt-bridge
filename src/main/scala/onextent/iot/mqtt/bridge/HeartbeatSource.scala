package onextent.iot.mqtt.bridge

import akka.actor.ActorSystem
import akka.stream.stage.{GraphStage, GraphStageLogic, OutHandler}
import akka.stream.{Attributes, Outlet, SourceShape}
import com.typesafe.scalalogging.LazyLogging
import onextent.iot.mqtt.bridge.models.Heartbeat
import onextent.iot.mqtt.bridge.Conf._

class HeartbeatSource(implicit system: ActorSystem)
    extends GraphStage[SourceShape[Heartbeat]] with LazyLogging {

  val out: Outlet[Heartbeat] = Outlet("HeartbeatSource")

  override val shape: SourceShape[Heartbeat] = SourceShape(out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic =
    new GraphStageLogic(shape) {

      setHandler(
        out,
        new OutHandler {
          override def onPull(): Unit = {
            push(out, Heartbeat(myName))
          }
        }
      )
    }

}
