import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorContext, ActorSystem, Behavior, ExtensibleBehavior, Signal}

object TypedSample3 extends App {

  sealed trait Command
  case class Hello(who: String) extends Command
  case class ChangeGreeting(newGreeting: String) extends Command

  class GreetingBehavior(private var greeting: String) extends ExtensibleBehavior[Command] {

    def receive(ctx: ActorContext[Command], msg: Command): Behavior[Command] = {
      msg match {
        case Hello(who) =>
          println(s"$greeting $who!")
          Behaviors.same
        case ChangeGreeting(newGreeting) =>
          greeting = newGreeting
          Behaviors.same
      }
    }

    def receiveSignal(ctx: ActorContext[Command], msg: Signal): Behavior[Command] = {
      Behaviors.same
    }

  }

  val system = ActorSystem(new GreetingBehavior("Hello"), "my-system")

  system ! Hello("Johan")
  system ! ChangeGreeting("Buon Pomeriggio")
  system ! Hello("Scala Italy")

}
