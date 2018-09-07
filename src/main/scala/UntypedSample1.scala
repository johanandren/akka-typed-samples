import akka.actor.{Actor, ActorSystem, Props}

object UntypedSample1 extends App {

  case class Hello(who: String)

  class GreetingActor extends Actor {
    def receive = {
      case Hello(who) => println(s"Hello $who!")
    }
  }
  val props = Props(new GreetingActor)

  val system = ActorSystem("my-system")
  val ref = system.actorOf(props, "greeter")

  ref ! Hello("Johan")
  ref ! Hello("Scala Italy")
  ref ! "ups"

  // (messages delivered async, main completes, system keeps running)
}
