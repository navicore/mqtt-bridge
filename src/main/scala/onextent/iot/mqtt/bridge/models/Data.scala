package onextent.iot.mqtt.bridge.models

sealed trait Command

case class Heartbeat(myName: String) extends Command {
  def beat(): String = s"Heartbeat from $myName"
  def asJson(): String = s"""{"msg": "${beat()}"}"""
}

