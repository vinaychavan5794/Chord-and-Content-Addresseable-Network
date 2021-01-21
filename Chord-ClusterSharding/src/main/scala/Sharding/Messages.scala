package Sharding
import Domain.{Container, Junction}
object Messages {
  case class Go(targetConveyor:String)
  case class WhereShouldIGo(junction: Junction, container: Container)
}
