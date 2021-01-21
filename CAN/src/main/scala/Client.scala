import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Serializable
import java.net.InetAddress
import java.net.UnknownHostException
import java.rmi.AlreadyBoundException
import java.rmi.NotBoundException
import java.rmi.RemoteException
import java.rmi.registry.LocateRegistry
import java.rmi.registry.Registry
import java.rmi.server.UnicastRemoteObject
import java.text.SimpleDateFormat
import java.util.{Calendar, HashSet, Iterator, LinkedList, Queue, Scanner}
import java.util
import java.util.logging.Logger

import akka.actor.{Actor, ActorRef}
import akka.serialization.NullSerializer.identifier

import scala.collection.convert.ImplicitConversions.`iterator asJava`
import scala.collection.mutable
import scala.util.control.Breaks.break


/**
 * Class Client implements functionality for peers in a CAN system. It has
 * functions to join, leave, view peers, add, search and list files.
 *
 *
 *
 */
@SerialVersionUID(1L)
object Client {
  /**
   * main
   *
   *
   */

    var count = 0;
  var idToClientMap = new mutable.HashMap[Int,Client]()
  var clientToActorMap = new mutable.HashMap[Client,ActorRef]()
  val logger:Logger = Logger.getLogger("log")
  
  def main(args: Array[String]): Unit = {
    try {
      var me = new Client(count)
      me.BSIPAddress = InetAddress.getLocalHost.getHostAddress
      @SuppressWarnings(Array("resource")) val sc = new Scanner(System.in)
      var choice = 0
      while ( {
        true
      }) {
        logger.info("1. Join CAN")
        logger.info("2. Leave CAN")
        logger.info("3. View Neighbours")
        logger.info("4. View information of peer by ip")
        logger.info("5. Insert file")
        logger.info("6. Search for file")
        logger.info("7. View files in current peer")
        logger.info("8. Exit")
        choice = sc.nextInt
        choice match {
          case 1 =>
            logger.info("Trying to join")
            if (!me.joinFlag) {
              me.join()
              logger.info("Node info: " + me)

              count = count+1
              me = new Client(count)
              me.BSIPAddress = InetAddress.getLocalHost.getHostAddress
            }
            else logger.info("Already Connected to CAN")

          case 2 =>
            if (me.joinFlag) {
              logger.info("Leaving CAN")
              me.leave()
            }
            else logger.info("Can't leave without joining")

          case 3 =>
            if (me.joinFlag) {
              logger.info("Obtaining neighbours")
              val iterator = me.neighbours.iterator
              var temp = new Neighbour
              while ( {
                iterator.hasNext
              }) {
                temp = iterator.next
                logger.info(" "+temp)
              }
            }
            else logger.info("Can't obtain neighbours without joining")

          case 4 =>
            if (me.joinFlag) {
              logger.info("Enter Node identifier: ")
              val id = sc.next
              if (id == me.identifier) logger.info("Node info: " + me)
              else me.getIPs(id)
            }
            else logger.info("Can't view nodes without joining")

          case 5 =>
            if (me.joinFlag) {
              logger.info("Enter filename to be inserted: ")
              me.storeFile(sc.next)
            }
            else logger.info("Can't add file without joining")

          case 6 =>
            if (me.joinFlag) {
              logger.info("Enter filename to be searched: ")
              me.searchFile(sc.next)
            }
            else logger.info("Can't search for file without joining")

          case 7 =>
            if (me.joinFlag) {
              logger.info("File list: ")
              val iterator = me.files.iterator
              var temp = ""
              while ( {
                iterator.hasNext
              }) {
                temp = iterator.next
                logger.info(temp)
              }
            }
            else logger.info("Can't see file list without joining")

          case 8 =>
            if (!me.joinFlag) {
              logger.info("Exit")
              System.exit(0)
            }
            else logger.info("Can't exit without leaving")

          case _ =>
            logger.info("Oops wrong choice, try again!")

        }
      }
    } catch {
      case e: RemoteException =>
        logger.info("Remote Exception in Client")
        e.printStackTrace()
      case e: UnknownHostException =>
        logger.info("Unknown host Exception in Client")
        e.printStackTrace()
    }
  }
}

@SerialVersionUID(1L)
class Client(id:Int) extends UnicastRemoteObject with Serializable {
  val logger:Logger = Logger.getLogger("log")
  var BSIPAddress:String = null
  var identifier:String = null
  var files : mutable.HashSet[String] = new mutable.HashSet[String]()
  var joinFlag = false
  var myCoordinate: Coordinate = null
  var myIPAddress = "192.168.1."+(6+id)//.substring(0,Bootstrap.bootstrapNode_ip.lastIndexOf('.'))+(Bootstrap.bootstrapNode_ip.substring(Bootstrap.bootstrapNode_ip.lastIndexOf('.')+1).toInt+id) ;
  var myPeer:Registry = null
  var neighbours : mutable.HashSet[Neighbour] = new mutable.HashSet[Neighbour]()
  /**
   * Global shared variables
   */


  override def toString = {
    var s = ""
    s += "My IP: " + myIPAddress + "\n"
    s += "My indentifier: " + identifier + "\n"
    if (joinFlag) s += "Node is connected\n"
    s += "\nMy Coordinates: " + myCoordinate
    s += "Files list: \n"
    if (!files.isEmpty) {
      val fileIterator = files.iterator
      while ( {
        fileIterator.hasNext
      }) {
        val file = fileIterator.next
        s += file + "\n"
      }
    }
    else s += "Empty"
    s += "\nNeighbour list: \n"
    if (!neighbours.isEmpty) {
      val neighbourIterator = neighbours.iterator
      while ( {
        neighbourIterator.hasNext
      }) {
        val n = neighbourIterator.next
        s += n
      }
    }
    else s += "Empty"
    s
  }

  /**
   * Hash Function to generate x coordinate
   *
   * @param keyword
   * @return double value
   */
  private def CharAtOdd(keyword: String) = {
    var sum = 0
    if (keyword.length == 1) 0
    else {
      var i = 0
      while ( {
        i < keyword.toCharArray.length
      }) {
        sum = sum + keyword.charAt(i)
        i = i + 2
      }
      sum % 10
    }
  }

  /**
   * Hash Function to generate y coordinate
   *
   * @param keyword
   * @return double value
   */
  private def CharAtEven(keyword: String) = {
    var sum = 0
    if (keyword.length == 1) 1
    else {
      var i = 1
      while ( {
        i < keyword.toCharArray.length
      }) {
        sum = sum + keyword.charAt(i)
        i = i + 2
      }
      sum % 10
    }
  }

  /**
   * join method adds this node to the CAN network
   */
  def join(): Unit = {
    try {
      Client.count = Client.count+1
      val bsServer = LocateRegistry.getRegistry(BSIPAddress, 21391)
      val obj = bsServer.lookup("Bootstrap Server").asInstanceOf[BootstrapInterface]
      val bsIP = obj.getIPAddress(myIPAddress)
      myPeer = LocateRegistry.createRegistry(21392+id)
      myPeer.bind("peer"+id, this)
      identifier = new SimpleDateFormat("HHmmss").format(Calendar.getInstance.getTime)
      if (bsIP == null) {
        logger.info("First Node in CAN")
        myCoordinate = new Coordinate(0, 0, 10, 10)
        obj.setIPAddress(myIPAddress)
      }
      else {
        val peer2: Registry = LocateRegistry.getRegistry(bsIP, 21391)
        val peer:Client = peer2.lookup("peer").asInstanceOf[Client]
        val randomint_x = obj.getRandomCoordinate
        val randomint_y = obj.getRandomCoordinate
        val s = peer.routeNode(randomint_x, randomint_y, myIPAddress)
        logger.info("Added " + s)
      }
      joinFlag = true
      logger.info("Joined Successfully\n")
    } catch {
      case e: RemoteException =>
        logger.info("Error in join on " + myIPAddress)
        e.printStackTrace()
      case e: NotBoundException =>
        logger.info("Error in join on " + myIPAddress)
        e.printStackTrace()
      case e: AlreadyBoundException =>
        logger.info("Error in join on " + myIPAddress)
        e.printStackTrace()
    }
  }

  /**
   * splitCurrentSpace splits the current space to make room for new peers
   *
   * @param incomingIP
   * ip address of the incoming node
   */
  private def splitCurrentSpace(incomingIP: String): Unit = {
    logger.info("Split space for: " + incomingIP)
    try {
      val incomingRegistry = LocateRegistry.getRegistry(incomingIP, 21391)
      val incomingPeer = incomingRegistry.lookup("peer").asInstanceOf[Client]
      // Perform split and assign new coordinates
      if (myCoordinate.splitVertically) {
        val x = myCoordinate.upperX
        myCoordinate.performSplitVertically()
        incomingPeer.setCoordinate(new Coordinate(myCoordinate.upperX, myCoordinate.lowerY, x, myCoordinate.upperY))
      }
      else {
        val y = myCoordinate.upperY
        myCoordinate.performSplitHorizontally()
        incomingPeer.setCoordinate(new Coordinate(myCoordinate.lowerX, myCoordinate.upperY, myCoordinate.upperX, y))
      }
      // Reassigning the files based on coordinates
      val iterator = files.iterator
      while ( {
        iterator.hasNext
      }) {
        val file = iterator.next
        val fileX = CharAtOdd(file)
        val fileY = CharAtEven(file)
        if (!myCoordinate.contains(fileX, fileY)) {
          logger.info("File " + file + " moved to " + incomingPeer.getIdentifier)
          iterator.remove()
          files.remove(file)
          incomingPeer.addFile(file, myIPAddress)
        }
      }
      // Updating the neighbours
      val incomingAsNeighbour = new Neighbour(incomingPeer.getIPAddress, incomingPeer.getIdentifier, incomingPeer.getCoordinate)
      addNeighbour(incomingAsNeighbour)
      val selfAsNeighbour = new Neighbour(myIPAddress, identifier, myCoordinate)
      incomingPeer.addNeighbour(selfAsNeighbour)
      var temp = new Neighbour
      var neighbourRegistry:Registry = null
      if (neighbours.size > 1) {
        logger.info("Reassigning Neighbours")
        val iter = neighbours.iterator
        while ( {
          iter.hasNext
        }) {
          temp = iter.next
          val c = incomingPeer.getCoordinate
          neighbourRegistry = LocateRegistry.getRegistry(temp.ip, 21391)
          val tempNode = neighbourRegistry.lookup("peer").asInstanceOf[Client]
          if (((c.isAdjacentX(temp.c) && c.isSubsetY(temp.c)) || (c.isAdjacentY(temp.c) && c.isSubsetX(temp.c))) && (!(temp.ip == incomingIP))) {
            logger.info("Adding node " + incomingPeer.getIdentifier + " to neighbour " + temp.identifier)
            incomingPeer.addNeighbour(temp)
            tempNode.addNeighbour(incomingAsNeighbour)
          }
          if ((myCoordinate.isAdjacentX(temp.c) && myCoordinate.isSubsetY(temp.c)) || (myCoordinate.isAdjacentY(temp.c) && myCoordinate.isSubsetX(temp.c))) tempNode.updateNeighbour(selfAsNeighbour)
          else {
            logger.info("Removing neighbour " + temp.identifier)
            iter.remove()
            neighbours.remove(temp)
            tempNode.deleteNeighbour(selfAsNeighbour)
          }
        }
      }
    } catch {
      case e: NotBoundException =>
        logger.info("Error in splitCurrentSpace on " + myIPAddress + " for ip " + incomingIP)
        e.printStackTrace()
      case e: RemoteException =>
        logger.info("Error in splitCurrentSpace on " + myIPAddress + " for ip " + incomingIP)
        e.printStackTrace()
    }
  }

  /**
   * checkNeighbour finds the neighbour with the best match in terms of size
   *
   * @return the neighbour with the best match
   */
  private def checkNeighbour: Neighbour = {
    var myNeighbour:Neighbour = null
    val iter = neighbours.iterator
    while ( {
      iter.hasNext
    }) {
      myNeighbour = iter.next
      if (myCoordinate.isSameSize(myNeighbour.c)) return myNeighbour
    }
    null
  }

  /**
   * leave enables this node to leave and transfer its files to other
   * neighbours
   */
   def leave(): Unit = {
    if (neighbours.isEmpty) {
      var bsServer:Registry = null
      try {
        bsServer = LocateRegistry.getRegistry(BSIPAddress, 21391)
        val obj = bsServer.lookup("Bootstrap Server").asInstanceOf[BootstrapInterface]
        files.clear()
        obj.setIPAddress(null)
        UnicastRemoteObject.unexportObject(myPeer, true)
        joinFlag = false
        logger.info("Node left from the CAN ")
        return
      } catch {
        case e: RemoteException =>
          logger.info("Error in leave on " + myIPAddress)
          e.printStackTrace()
        case e: NotBoundException =>
          logger.info("Error in leave on " + myIPAddress)
          e.printStackTrace()
      }
    }
    // Find the best neighbour to merge with
    val myNeighbour = checkNeighbour
    if (myNeighbour != null) try {
      val neighbourRegistry = LocateRegistry.getRegistry(myNeighbour.ip, 21391)
      val peer = neighbourRegistry.lookup("peer").asInstanceOf[Client]
      val bsServer = LocateRegistry.getRegistry(BSIPAddress, 21391)
      val obj = bsServer.lookup("Bootstrap Server").asInstanceOf[BootstrapInterface]
      // Update bootstrap ip if the leaving node was the bootstrap
      // peer
      if (myIPAddress == obj.getIPAddress(myIPAddress)) {
        logger.info("Update Bootstrp peer")
        obj.setIPAddress(myNeighbour.ip)
      }
      // Reassign the files directly since the coordinates will be
      // merged
      val iterator = files.iterator
      while ( {
        iterator.hasNext
      }) {
        val filename = iterator.next
        peer.addFile(filename, myIPAddress)
        iterator.remove()
        files.remove(filename)
      }
      // Update the coordinates of the neighbour
      var c = peer.getCoordinate
      // logger.info("old c: " + c);
      c = myCoordinate.updateCoordinate(c)
      // logger.info("new c: " + c);
      peer.setCoordinate(c)
      myNeighbour.c = c
      // Update the neighbours to reflect the change in coordinates
      // and add my neighbours to the merged zone
      var tempNeighbour = new Neighbour
      var newNeighbourRegistry:Registry = null
      var iter = neighbours.iterator
      while ( {
        iter.hasNext
      }) {
        tempNeighbour = iter.next
        newNeighbourRegistry = LocateRegistry.getRegistry(tempNeighbour.ip, 21391)
        val tempNode = newNeighbourRegistry.lookup("peer").asInstanceOf[Client]
        if ((c.isAdjacentX(tempNeighbour.c) && c.isSubsetY(tempNeighbour.c)) || (c.isAdjacentY(tempNeighbour.c) && c.isSubsetX(tempNeighbour.c))) {
          tempNode.addNeighbour(myNeighbour)
          tempNode.deleteNeighbour(new Neighbour(myIPAddress, identifier, myCoordinate))
        }
        else {
          logger.info("Removing neighbour " + tempNeighbour.identifier)
          tempNode.deleteNeighbour(myNeighbour)
          tempNode.deleteNeighbour(new Neighbour(myIPAddress, identifier, myCoordinate))
        }
        // Remove neighbours from self
        iter.remove()
        neighbours.remove(tempNeighbour)
      }
      // Update the coordinates for myNeighbour's neighbours
      val myNeighbourNeighbours = peer.getNeighbours
      iter = myNeighbourNeighbours.iterator
      while ( {
        iter.hasNext
      }) {
        tempNeighbour = iter.next
        newNeighbourRegistry = LocateRegistry.getRegistry(tempNeighbour.ip, 21391)
        val tempNode = newNeighbourRegistry.lookup("peer").asInstanceOf[Client]
        tempNode.updateNeighbour(myNeighbour)
      }
      // Export object to end RMI connection and reset join value
      UnicastRemoteObject.unexportObject(myPeer, true)
      joinFlag = false
    } catch {
      case e: RemoteException =>
        logger.info("Error in leave on " + myIPAddress)
        e.printStackTrace()
      case e: NotBoundException =>
        logger.info("Error in leave on " + myIPAddress)
        e.printStackTrace()
    }
    else logger.info("Failure")
  }

  /**
   * Stores a file from a particular node by figuring out it's coordinates
   *
   * @param filename
   * of the file
   */
   def storeFile(filename: String): Unit = { // logger.info("Saving file on " + identifier);
    val x = CharAtOdd(filename)
    val y = CharAtEven(filename)
    val f = new File(filename)
    // Checks if the file actually present
    if (!f.exists && !f.isDirectory) if (myCoordinate.contains(x, y)) addFile(filename, myIPAddress)
    else try routeFile(x, y, filename, myIPAddress)
    catch {
      case e: RemoteException =>
        logger.info("Error in saveFile on " + myIPAddress)
        e.printStackTrace()
    }
    else logger.info("File does not exist, Failure")
  }

  /**
   * Search for a file
   *
   * @param filename
   * name of the file
   */
   def searchFile(filename: String): Unit = {
    val x = CharAtOdd(filename)
    val y = CharAtEven(filename)
    var ip:String = null
    try {
      ip = findFile(x, y, filename)
      if (ip != null && !ip.contains("Failure")) logger.info("File found on " + ip)
      else logger.info("File Not Found, Failure")
    } catch {
      case e: RemoteException =>
        logger.info("Eror in searchFile on " + myIPAddress)
        e.printStackTrace()
    }
  }

  /**
   * getClosestNeighbour finds the closest neighbour for given coordinates
   *
   * @param x
   * @param y
   * given set of coordinates
   * @return a neighbour close to the input coordinates
   */
  private def getClosestNeighbour(x: Double, y: Double) = {
    var neighbouringPeer = new Neighbour
    var distance = Double.MaxValue
    val iterator = neighbours.iterator
    var temp = new Neighbour
    while ( {
      iterator.hasNext
    }) {
      temp = iterator.next
      if (temp.c.distance(x, y) <= distance) {
        distance = temp.c.distance(x, y)
        neighbouringPeer = temp
      }
    }
    neighbouringPeer
  }

  /**
   * display method displays a peer's information
   *
   * @param ip
   */
  private def display(ip: String): Unit = {
    try {
      val connection = LocateRegistry.getRegistry(ip, 21391)
      val peer:Client = connection.lookup("peer").asInstanceOf[Client]
      var s = "\nIP " + peer.getIPAddress + "\n"
      if (peer.hasJoined) s += "Node is connected\n"
      s += "Coordinates: " + peer.getCoordinate
      s += "Files list: \n"
      if (!files.isEmpty) {
        val fileIterator = peer.getFileList.iterator
        while ( {
          fileIterator.hasNext
        }) {
          val file = fileIterator.next
          s += file + "\n"
        }
      }
      else s += "Empty"
      s += "\nNeighbour list: \n"
      if (!neighbours.isEmpty) {
        val neighbourIterator = peer.getNeighbours.iterator
        while ( {
          neighbourIterator.hasNext
        }) {
          val n = neighbourIterator.next
          s += n
        }
      }
      else s += "Empty"
      logger.info("Node info: " + s)
    } catch {
      case e: NotBoundException =>
        logger.info("Error in display of " + ip)
        e.printStackTrace()
      case e: RemoteException =>
        logger.info("Error in display of " + ip)
        e.printStackTrace()
    }
  }

  /**
   * getIPs collects the details for all the peers in the CAN system
   *
   * @param id
   * id is the string to that represents the node to be displayed
   * or all nodes
   */
  @throws[RemoteException]
  private def getIPs(id: String): Unit = {
    val globalPeers: mutable.HashSet[Neighbour] = new mutable.HashSet[Neighbour]
    val q = new util.LinkedList[Neighbour]
    // Add self
    globalPeers.add(new Neighbour(myIPAddress, identifier, myCoordinate))
    for (n <- neighbours) {
      q.add(n)
      globalPeers.add(n)
    }
    var tempNeighbour = new Neighbour
    // Add all the other nodes
    while ( {
      !q.isEmpty
    }) {
      tempNeighbour = q.remove
      try {
        val connection = LocateRegistry.getRegistry(tempNeighbour.ip, 21391)
        val peer = connection.lookup("peer").asInstanceOf[Client]
        val current = peer.getNeighbours
        for (currentNeighbour <- current) {
          if (!globalPeers.contains(tempNeighbour)) q.add(currentNeighbour)
        }
      } catch {
        case e: NotBoundException =>
          logger.info("Error in getIPs of " + myIPAddress)
          e.printStackTrace()
      }
    }
    // Provide different views
    if (id == "view") {
      for (each <- globalPeers) {
        display(each.ip)
      }
    }
    else {
      for (each <- globalPeers) {
        if (each.identifier == id) {
          display(each.ip)
          break //todo: break is not supported

        }
      }
    }
  }

  /**
   * getCoordinate
   *
   * @return my coordinates
   */
  @throws[RemoteException]
  def getCoordinate = { // logger.info("Getting coordinate on " + myIPAddress);
    myCoordinate
  }

  /**
   * setCoordinate
   *
   * @param c
   * coordinate to be set
   */
  @throws[RemoteException]
  def setCoordinate(c: Coordinate): Unit = { // logger.info("Setting coordinate on " + myIPAddress);
    myCoordinate = c
  }

  /**
   * getIPAddress
   *
   * @return string containing the IP address
   */
  @throws[RemoteException]
  def getIPAddress = { // logger.info("Getting ip on " + myIPAddress);
    myIPAddress
  }

  /**
   * getIdentifier
   *
   * @return string containing the IP address
   */
  @throws[RemoteException]
  def getIdentifier = { // logger.info("Getting identifier on " + identifier);
    identifier
  }

  /**
   * getNeighbours
   *
   * @return set of neighbours
   */
  @throws[RemoteException]
  def getNeighbours = { // logger.info("Getting neighbours on " + myIPAddress);
    neighbours
  }

  /**
   * addNeighbour if not already present else update it
   *
   * @param n
   * neighbour to be added
   */
  @throws[RemoteException]
  def addNeighbour(n: Neighbour): Unit = {
    logger.info("Adding neighbour " + n.identifier + " on " + identifier)
    if (neighbours.contains(n)) {
      logger.info("Neighbour already present. Updating")
      updateNeighbour(n)
      return
    }
    neighbours.add(n)
  }

  /**
   * updateNeighbour updates the coordinates of given neighbour
   *
   * @param n
   * neighbour to be updated
   */
  @throws[RemoteException]
  def updateNeighbour(n: Neighbour): Unit = {
    val iterator = neighbours.iterator
    var temp = new Neighbour
    while ( {
      iterator.hasNext
    }) {
      temp = iterator.next
      if (temp.ip == n.ip) { // logger.info("Found " + temp.ip);
        logger.info("Update neighbour " + n.identifier + " on " + identifier)
        temp.c = n.c
        return
      }
    }
  }

  /**
   * deleteNeighbour deletes a neighbour
   *
   * @param n
   * neighbour to be deleted
   */
  @throws[RemoteException]
  def deleteNeighbour(n: Neighbour): Unit = {
    neighbours.remove(n)
  }

  /**
   * getFileList gets set of files
   *
   * @return set of files
   */
  @throws[RemoteException]
  def getFileList = { // logger.info("Getting file list on " + myIPAddress);
    files
  }

  /**
   * addFile adds the file to a node
   *
   * @param filename
   * name of the file
   * @param originatingIP
   * ip of the originating node
   */
  def addFile(filename: String, originatingIP: String): Unit = {
    if (originatingIP == myIPAddress) {
      files.add(filename)
      logger.info("Adding file " + filename + " on " + identifier)
    }
    else try {
      val fileRegistry = LocateRegistry.getRegistry(originatingIP, 21391)
      val peer = fileRegistry.lookup("peer").asInstanceOf[Client]
      val filedata = peer.saveFile(filename)
      val file = new File(filename)
      val output = new BufferedOutputStream(new FileOutputStream(file.getName))
      output.write(filedata, 0, filedata.length)
      output.flush()
      output.close()
      files.add(filename)
      logger.info("Adding file " + filename + " on " + identifier)
    } catch {
      case e: Exception =>
        System.err.println("Error in addFile on " + myIPAddress)
        e.printStackTrace()
    }
  }

  /**
   * saveFile gets the file from the originating ip
   *
   *
   * name of the file
   * @return byte array containing the contents of the file
   */
  def saveFile(fileName: String) = { // logger.info("Saving file " + fileName + " from " +
    // myIPAddress);
    try {
      val file = new File(fileName)
      val buffer = new Array[Byte](file.length.toInt)
      val input = new BufferedInputStream(new FileInputStream(fileName))
      input.read(buffer, 0, buffer.length)
      input.close()
      buffer
    } catch {
      case e: Exception =>
        logger.info("saveFile on + " + myIPAddress + ": " + e.getMessage)
        e.printStackTrace()
        null
    }
  }

  /**
   * routeFile routes the file from current node to end node
   *
   * @param x
   * @param y
   * x and y are the coordinates for the file
   * @param filename
   * name of the file
   * @param originatingIP
   * ip of the originating node
   */
  @throws[RemoteException]
  def routeFile(x: Double, y: Double, filename: String, originatingIP: String): Unit = { // logger.info("Routing file on " + myIPAddress);
    if (myCoordinate.contains(x, y)) addFile(filename, originatingIP)
    else {
      val neighbouringClient = getClosestNeighbour(x, y)
      val neighbrIP = neighbouringClient.ip
      try {
        val connection = LocateRegistry.getRegistry(neighbrIP, 21391)
        val peer = connection.lookup("peer").asInstanceOf[Client]
        peer.routeFile(x, y, filename, originatingIP)
      } catch {
        case e: NotBoundException =>
          logger.info("Error in routeFile on " + myIPAddress)
          e.printStackTrace()
      }
    }
  }

  /**
   * findFile finds the file from current node to end node
   *
   * @param x
   * @param y
   * x and y are the coordinates for the file
   * @param filename
   * name of the file
   */
  @throws[RemoteException]
  def findFile(x: Double, y: Double, filename: String): String = { // logger.info("Searching for " + filename + " on " +
    if (myCoordinate.contains(x, y)) if (files.contains(filename)) return myIPAddress + " with route: " + identifier
    else return "Failure"
    else {
      val neighbouringClient = getClosestNeighbour(x, y)
      val neighbrIP = neighbouringClient.ip
      try {
        val connection = LocateRegistry.getRegistry(neighbrIP, 21391)
        val peer = connection.lookup("peer").asInstanceOf[Client]
        return peer.findFile(x, y, filename) + "<--" + identifier
      } catch {
        case e: NotBoundException =>
          logger.info("Error in findFile on " + myIPAddress)
          e.printStackTrace()
      }
    }
    null
  }

  /**
   * routeNode routes the node from current node to end node
   *
   * @param x
   * @param y
   * x and y are the coordinates for the file
   * @param incomingIP
   * ip of the incoming node
   */
  @throws[RemoteException]
  def routeNode(x: Double, y: Double, incomingIP: String): String = {
    logger.info("Incoming to route node " + incomingIP + " with x: " + x + " y: " + y)
    if (myCoordinate.contains(x, y)) {
      splitCurrentSpace(incomingIP)
      return myIPAddress + " with route: " + identifier
    }
    else { // logger.info("Forwarding");
      val neighbouringPeer = getClosestNeighbour(x, y)
      try {
        val connection = LocateRegistry.getRegistry(neighbouringPeer.ip, 21391)
        val peer = connection.lookup("peer").asInstanceOf[Client]
        return peer.routeNode(x, y, incomingIP) + "<--" + identifier
      } catch {
        case e: NotBoundException =>
          logger.info("Error in routeNode on " + myIPAddress)
          e.printStackTrace()
      }
    }
    null
  }

  /**
   * has joined
   *
   * @return true if joined
   */
  def hasJoined = joinFlag
}