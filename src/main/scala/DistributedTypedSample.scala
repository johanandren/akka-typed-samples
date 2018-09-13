import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.receptionist.Receptionist.{Listing, Register, Subscribe}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, Join}
import com.typesafe.config.ConfigFactory
import scala.concurrent.duration._

// Note: in a real application (tm) you'd probably not want to put all the things
// in one file like this, it is only to keep the separate clearly separated and
// easy to read
object DistributedTypedSample extends App {

  object Alarm {

    sealed trait AlarmMessage
    case object EnableAlarm extends AlarmMessage
    case class DisableAlarm(pinCode: String) extends AlarmMessage
    case class ActivitySeen(what: String) extends AlarmMessage

    val serviceKey = ServiceKey[ActivitySeen]("alarm")

    def distributedAlarm(pinCode: String): Behavior[AlarmMessage] =
      Behaviors.setup { ctx =>
        ctx.log.info("Starting up alarm")
        ctx.system.receptionist ! Register(serviceKey, ctx.self)

        enabledAlarm(pinCode)
      }

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
  }



  object Sensor {

    sealed trait SensorMessage
    case object Trigger extends SensorMessage
    case class AlarmsChanged(alarms: Set[ActorRef[Alarm.ActivitySeen]])
      extends SensorMessage

    def sensor(where: String): Behavior[SensorMessage] =
      Behaviors.setup { ctx =>
        val adapter = ctx.messageAdapter[Listing] {
          case Alarm.serviceKey.Listing(alarms) =>
            AlarmsChanged(alarms)
        }

        ctx.system.receptionist ! Subscribe(Alarm.serviceKey, adapter)

        sensorWithAlarms(where, Set.empty)
      }

    def sensorWithAlarms(
      where: String,
      alarms: Set[ActorRef[Alarm.ActivitySeen]]
    ): Behavior[SensorMessage] =
      Behaviors.receive { (ctx, msg) =>
        msg match {
          case Trigger =>
            val event = Alarm.ActivitySeen(where)
            alarms.foreach(_ ! event)
            Behaviors.same
          case Sensor.AlarmsChanged(newSetOfAlarms) =>
            ctx.log.info(s"New set of alarms: {}", newSetOfAlarms)
            sensorWithAlarms(where, newSetOfAlarms)
        }
      }
  }

  val config = ConfigFactory.parseString(
    """
      akka.actor.provider=cluster
      akka.remote.artery.enabled=true
      akka.remote.artery.transport=tcp
      akka.remote.artery.canonical.port=0 # get a port from the OS
      akka.remote.artery.canonical.hostname=127.0.0.1

      # these are ok just because this is a sample
      akka.cluster.jmx.multi-mbeans-in-same-jvm=on
      akka.actor.warn-about-java-serializer-usage=off
    """)

  import Alarm.distributedAlarm
  import Sensor.sensor
  // in a real system you would ofc not run multiple nodes in the same JVM
  val system1 = ActorSystem(distributedAlarm(pinCode = "0000"), "alarm", config)
  val system2 = ActorSystem(sensor("window"), "alarm", config)
  val system3 = ActorSystem(sensor("door"), "alarm", config)

  // programmatic cluster join
  val node1 = Cluster(system1)
  val node2 = Cluster(system2)
  val node3 = Cluster(system3)

  // node 1 joins itself to form cluster
  node1.manager ! Join(node1.selfMember.address)

  // the other two joins that cluster
  node2.manager ! Join(node1.selfMember.address)
  node3.manager ! Join(node1.selfMember.address)

  // blocking just to keep sample simple, don't do this!
  while(true) {
    Thread.sleep(3000)
    system2 ! Sensor.Trigger
    Thread.sleep(2000)
    system3 ! Sensor.Trigger
  }
}
