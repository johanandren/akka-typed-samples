import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration._

object BehaviorFactoriesSample {

  val behaviorWithSetup: Behavior[Unit] =
    Behaviors.setup { ctx =>
      ctx.log.info("Starting up")
      // other startup logic, for example
      // spawn children, subscribe, open resources

      // then return the actual behavior used
      Behaviors.empty
    }

  case class Message(text: String)
  val behaviorWithTimer: Behavior[Message] =
    Behaviors.withTimers { timers =>
      timers.startSingleTimer("single", Message("single"), 1.second)

      timers.startPeriodicTimer("repeated", Message("repeated"), 5.seconds)

      Behaviors.receive { (ctx, msg) =>
        ctx.log.info("Got msg: {}", msg)
        Behaviors.same
      }
    }


  case class Request(id: String)

  val behaviorWithMdc: Behavior[Request] =
    Behaviors.withMdc(
      Map("static" -> 5),
      (msg: Request) => Map("reqId" -> msg.id)
    ) {
      Behaviors.receive[Request]((ctx, msg) =>
        msg match {
          case _: Request =>
            // this will have the static and per-message mdc added
            ctx.log.info("got request")
            Behaviors.same
        })
    }


  val monitoringBehavior: Behavior[Message] = Behaviors.receive {(ctx, msg) =>
    ctx.log.info("Monitoring actor saw {}", msg)
    Behaviors.same
  }
  val monitoringActor: ActorRef[Message] = ??? // spawn monitoring behavior
  val actualBehavior: Behavior[Message] = Behaviors.ignore
  // also send every message to the actual behavior to the monitoring actor
  val monitoredBehavior = Behaviors.monitor(monitoringActor, actualBehavior)
}
