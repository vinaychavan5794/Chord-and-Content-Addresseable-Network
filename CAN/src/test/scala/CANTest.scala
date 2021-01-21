
import java.net.InetAddress
import java.rmi.registry.{LocateRegistry, Registry}

import org.junit.Assert
import org.scalatest.{BeforeAndAfter, FunSuite}


class CANTest extends FunSuite with BeforeAndAfter  {


  val bs = new Bootstrap
  val r: Registry = LocateRegistry.createRegistry(21491)
  r.bind("BootstrapServer", bs)
  val client = new Client(0)


  test("BootstrapBinding") {

    Assert.assertEquals("Array(BootstrapServer)",r.list().mkString("Array(", ", ", ")"))
  }

  test("RandomCoordinateCheck") {
    val obj=r.lookup("BootstrapServer").asInstanceOf[BootstrapInterface]

    Assert.assertNotNull(obj.getRandomCoordinate)
  }

  test("HostAddressCheck") {

    Assert.assertNotEquals(null,InetAddress.getLocalHost.getHostAddress)
  }


  test("ClientIP") {

    Assert.assertEquals("192.168.1.6",client.getIPAddress)
  }

  test("ClientNull") {

    Assert.assertNotNull(client)
  }


}
