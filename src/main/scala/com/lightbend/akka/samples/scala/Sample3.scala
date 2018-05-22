/**
 * Copyright (C) 2009-2018 Lightbend Inc. <https://www.lightbend.com>
 */
package com.lightbend.akka.samples.scala

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, MutableBehavior}

object Sample3 {
  sealed trait Command
  case class Hello(who: String) extends Command
  case class ChangeGreeting(newGreeting: String) extends Command

  class MutableGreetingBehavior(
      context: ActorContext[Command],
      var greeting: String
  ) extends MutableBehavior[Command] {

    override def onMessage(msg: Command): Behavior[Command] = {
      msg match {
        case Hello(who) =>
          context.log.info(s"$greeting ${who}!")
          Behaviors.same

        case ChangeGreeting(newGreeting) =>
          greeting = newGreeting
          Behaviors.same
      }
    }
  }


  def main(args: Array[String]): Unit = {
    val system = ActorSystem(
      Behaviors.setup[Command](
        context => new MutableGreetingBehavior(context, "Hello")),
      "my-system")

    system ! Hello("Johan")
    system ! ChangeGreeting("Sveiki")
    system ! Hello("Devdays Vilnius audience")
  }
}
