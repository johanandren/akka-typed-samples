import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object TypedSample4 extends App {

  sealed trait AlarmMessage
  case object EnableAlarm extends AlarmMessage
  case class DisableAlarm(pinCode: String) extends AlarmMessage
  case class ActivitySeen(what: String) extends AlarmMessage

  def enabledAlarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.receive { (actorCtx, msg) =>
      msg match {
        case ActivitySeen(what) =>
          actorCtx.log.warning("EOEOEOE, activity detected: {}", what)
          Behaviors.same

        case DisableAlarm(`pinCode`) =>
          actorCtx.log.info("Alarm disabled")
          disabledAlarm(pinCode)

        case DisableAlarm(_) =>
          actorCtx.log.warning("OEOEOE, unauthorized disable of alarm!")
          Behaviors.same

        case EnableAlarm =>
          Behaviors.unhandled

      }
    }

  def disabledAlarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.receive { (actorCtx, msg) =>
      msg match {
        case EnableAlarm =>
          enabledAlarm(pinCode)

        case _ =>
          Behaviors.unhandled
      }
    }


  val system = ActorSystem(enabledAlarm("0000"), "alarm")

  system ! ActivitySeen("door opened")
  system ! DisableAlarm("1234")
  system ! DisableAlarm("0000")
  system ! ActivitySeen("window opened")
  system ! EnableAlarm

}
