import InteractionSample.ActorB.{ProtocolB, RequestForB, ResponseFromB}
import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object InteractionSample extends App {

  object ActorA {

    sealed trait ProtocolA
    private case class GotResult(result: String) extends ProtocolA
    private case class GotNoResult(why: String) extends ProtocolA
    case object TriggerRequest extends ProtocolA

    def behavior(actorB: ActorRef[ProtocolB]): Behavior[ProtocolA] =
      Behaviors.setup { ctx =>
        // one off with timeout
        implicit val timeout: Timeout = 3.seconds
        ctx.ask(actorB)(RequestForB.apply) {
          case Success(ResponseFromB(result)) => GotResult(result)
          case Failure(error) => GotNoResult(error.getMessage)
        }

        val adapter: ActorRef[ResponseFromB] = ctx.messageAdapter {
          case ResponseFromB(result) => GotResult(result)
        }

        Behaviors.receiveMessage {
          case TriggerRequest =>
            actorB ! RequestForB(adapter)
            Behaviors.same
          case GotResult(result) =>
            ctx.log.info("Yay, result: {}", result)
            Behaviors.same
          case GotNoResult(why) =>
            ctx.log.info("Booo, no result: {}", why)
            Behaviors.same
        }
      }
  }

  object ActorB {
    sealed trait ProtocolB

    case class RequestForB(replyTo: ActorRef[ResponseFromB]) extends ProtocolB
    case class ResponseFromB(result: String)

    val behavior: Behavior[ProtocolB] =
      Behaviors.receiveMessage {
        case RequestForB(replyTo) =>
          replyTo ! ResponseFromB("ok!")
          Behaviors.same
      }

  }

  val rootBehavior: Behavior[Unit] = Behaviors.setup { ctx =>

    val b = ctx.spawn(ActorB.behavior, "b")
    val a = ctx.spawn(ActorA.behavior(b), "a")

    Behaviors.empty
  }

  val system = ActorSystem(rootBehavior, "sample-system")

}
