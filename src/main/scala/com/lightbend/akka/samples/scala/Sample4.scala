/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.scala

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object Sample4 {

  sealed trait AlarmMessage
  case class EnableAlarm(pinCode: String) extends AlarmMessage
  case class DisableAlarm(pinCode: String) extends AlarmMessage
  case object ActivityEvent extends AlarmMessage

  def enabledAlarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.receive { (context, message) =>
      message match {
        case ActivityEvent =>
          context.log.warning("EOEOEOEOEOE ALARM ALARM!!!")
          Behaviors.same

        case DisableAlarm(`pinCode`) =>
          context.log.info("Correct pin entered, disabling alarm");
          disabledAlarm(pinCode)
        case _ => Behaviors.unhandled
      }

    }

  def disabledAlarm(pinCode: String): Behavior[AlarmMessage] =
    Behaviors.receivePartial {
      case (context, EnableAlarm(`pinCode`)) =>
        context.log.info("Correct pin entered, enabling alarm")
        enabledAlarm(pinCode)
    }

  def main(args: Array[String]): Unit = {
    val system = ActorSystem.create(enabledAlarm("0000"), "my-system")
    system.tell(ActivityEvent)
    system.tell(DisableAlarm("1234"))
    system.tell(ActivityEvent)
    system.tell(DisableAlarm("0000"))
    system.tell(ActivityEvent)
    system.tell(EnableAlarm("0000"))
    system.tell(ActivityEvent)
  }

}
