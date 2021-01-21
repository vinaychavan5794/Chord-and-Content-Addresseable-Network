
import ServerActor.{addMeToRing, getPredecessor, getServerSnapshot, getSuccesor, loadMovie, lookUpKey, serverHashToPathMap, setPredecessor, setSuccessor, updateFingerTable, updateKeys}
import akka.actor.{Actor, ActorPath, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.language.postfixOps
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class ServerActor(serverId: Int,serverHash: Int,actorSystem:ActorSystem) extends Actor{

  var MAX_SERVERS = Driver.MAX_SERVERS
  //fingerTable :(key, key's (Successor hash value, Successor actor's path))

  private var currServerHashedValue: Int = serverHash
  implicit val timeout = Timeout(6 seconds)
  private val logger = Driver.logger
  private var serverCount = ServerActor.serverCount

  override def receive: Receive = {

    case addMeToRing(curServer:ActorRef) =>
      serverCount = ServerActor.serverCount
      currServerHashedValue = serverHash

      if (serverCount < MAX_SERVERS) {

        // if this is our first server to be added.
        if (ServerActor.fingerTables.isEmpty) {
          /**
           * self linking first server's successor and predecessor to itself
           */
          val previous = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))
          ServerActor.preNextKeys.put(currServerHashedValue,(previous._1,currServerHashedValue))

          val previous2 = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))
          ServerActor.preNextKeys.put(currServerHashedValue,(currServerHashedValue,previous2._2))
        } //if there's already a server in Ring.
        else {
          val serversHash =  ServerActor.serverActorHashedTreeSet
          val serversPath = ServerActor.serverHashToPathMap


          var curServersSuccessors = serversHash.filter(key => key > serverHash)

          if (curServersSuccessors.isEmpty) {
            curServersSuccessors = serversHash
          }
          val curServerSuccessor = actorSystem.actorSelection(
            serversPath.get(curServersSuccessors.head).getOrElse(""))
          val curServerPredecessorHash = ServerActor.preNextKeys.get(curServersSuccessors.head).getOrElse((-1,-1))._1
          val curServerPredecessor = actorSystem.actorSelection(serversPath.get(curServerPredecessorHash).getOrElse(""))

          /**
           * unlinking curServer's (predecessor and successor) and adding curServer between them
           */
          val previous = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._1
          ServerActor.preNextKeys.put(currServerHashedValue,(previous,curServersSuccessors.head))

          val previous2 = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._2
          ServerActor.preNextKeys.put(currServerHashedValue,(curServerPredecessorHash,previous2))

          curServerPredecessor ! setSuccessor(serverHash)
          curServerSuccessor ! setPredecessor(serverHash)


          val emptyList = (new mutable.TreeSet[Data]()(Data)).toList
          val list = curServerSuccessor ? updateKeys(emptyList, serverHash )
          val result = Await.result(list,timeout.duration)

          /**
           * update keys of curServer as to take control of few of our successor's keys which doesn't belong to it anymore.
           */
         // updateKeys(result.asInstanceOf[mutable.TreeSet[Data]].toList, serverHash)

        }

        /**
         * register this newly added server to our static sets in object class.
         */

        ServerActor.registerServer(serverHash,curServer.path.toString)

        //updating finger table
        val serversHash =  ServerActor.serverActorHashedTreeSet
        val serversPath = ServerActor.serverHashToPathMap
        serversHash.foreach(
          hash=> {
            /**
             * if else is to avoid self looping, if we want to update current server's finger table, why to make an asynchronously call.

             */

          if(hash != currServerHashedValue){
            val server = actorSystem.actorSelection(serversPath.get(hash).getOrElse(""))
            val future = server ? updateFingerTable()
            val result = Await.result(future,timeout.duration)

          }  else {
            // same logic of update finger table's
            val serversHash =  ServerActor.serverActorHashedTreeSet
            val serversPath = ServerActor.serverHashToPathMap
            val fingerTable = new mutable.TreeMap[Int,(Int,String)]()
            (0 to MAX_SERVERS-1).foreach(
              i => {
                val insertKey = (currServerHashedValue+math.pow(2,i))

                val successorsHash = serversHash.filter(key => key >= (insertKey % math.pow(2,MAX_SERVERS))).toList

                val successorHash = if(successorsHash.isEmpty) serversHash.head else successorsHash.head

                fingerTable += (insertKey.toInt -> (successorHash, serversPath.get(successorHash).getOrElse("")))
              }
            )
            ServerActor.fingerTables.put(currServerHashedValue,fingerTable)
          }

        })

        sender() ! "server: "+serverCount+" added succesfully with hashkey: "+serverHash
      } else{

        logger.info("server limit exceeded")
      }

    case updateFingerTable() => {
      val serversHash =  ServerActor.serverActorHashedTreeSet
      val serversPath = ServerActor.serverHashToPathMap
      val fingerTable = new mutable.TreeMap[Int,(Int,String)]()
      (0 to MAX_SERVERS-1).foreach(
        i => {
          val insertKey = (currServerHashedValue+math.pow(2,i))

          val successorsHash = serversHash.filter(key => key >= (insertKey % math.pow(2,MAX_SERVERS))).toList

          val successorHash = if(successorsHash.isEmpty) serversHash.head else successorsHash.head

          fingerTable += (insertKey.toInt -> (successorHash, serversPath.get(successorHash).getOrElse("")))
        }
      )
      ServerActor.fingerTables.put(currServerHashedValue,fingerTable)
      sender() ! "success"
    }

    case updateKeys(list:List[Data],key:Int)   => {

      val movies = ServerActor.movies.get(currServerHashedValue).getOrElse(new mutable.TreeSet[Data]()(Data))
      // for newly formed server as it's movie keys are empty
      if (movies.isEmpty) {
        movies.addAll(list)
        sender() ! movies
      }// old server
      else {
        val temp = movies.filter(movie=> movie.hash_key<=key)
        temp.foreach(movie => movies -= movie)
        sender() ! temp
      }
    }

    case lookUpKey(movie:Data)=> {
      val serversHash =  ServerActor.serverActorHashedTreeSet

      //if it's server supervisor
      if (serverId == 0) {
        val firstServer = actorSystem.actorSelection(ServerActor.serverHashToPathMap.get(serversHash.head).getOrElse(""))
        val future = firstServer ? lookUpKey(movie)
        val result = Await.result(future, timeout.duration)
        sender() ! result
      } else {
        val temp = new mutable.TreeMap[Int,(Int,String)]()
        val t = new mutable.TreeSet[Data]()(Data)
        val fingerTable = ServerActor.fingerTables.get(currServerHashedValue).getOrElse(temp)
        val movies = ServerActor.movies.get(currServerHashedValue).getOrElse(t)
        val currServersPredecessorHashKey = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._1

       if (movies.filter(m => m.movie_name.equals(movie.movie_name)).size >0){
          sender() ! ("movie: "+movie.movie_name+" with hash key: "+movie.hash_key+" found at server "+serverId)

        }
        /**
         *This else-if is to handle a corner case if movie's hash key > first server's hash key and is between first server and last server in CHORD.
         */
       else if(currServerHashedValue >= movie.hash_key || (currServersPredecessorHashKey > currServerHashedValue && movie.hash_key >currServersPredecessorHashKey)  || currServerHashedValue == currServersPredecessorHashKey) {
          sender() ! ("movie: "+movie.movie_name+" doesn't exist")
        } else {

          val closestPredecessors = fingerTable.filter(entry=>
            (entry._2._1 <= movie.hash_key && entry._2._1 > currServerHashedValue)
          )
          val closestPredecessorTuple = if(closestPredecessors.isEmpty) null else closestPredecessors.last._2

         /**
          * search for closest predecessor in our finger table and look-up movie in it.
          * if no closest predecessor found, this means your successor is the one who should look-up.
          */
         if (closestPredecessorTuple == null) {

            val successorPath = fingerTable.filter(entry=>{
              entry._2._1 >= movie.hash_key}).head._2._2

            val successor = actorSystem.actorSelection(successorPath)
            val future = successor ? lookUpKey(movie)
            val result = Await.result(future, timeout.duration)
            sender() ! result

          }
          else {

            var closestPredecessor = actorSystem.actorSelection(serverHashToPathMap.get(closestPredecessorTuple._1).getOrElse(""))

            val future = closestPredecessor ? lookUpKey(movie)
            val result = Await.result(future, timeout.duration)
            sender() ! result
          }
        }
      }
    }

    case loadMovie(movie:Data) => {
      var currServerHashedValue = serverHash
      val serversHash =  ServerActor.serverActorHashedTreeSet
      val serversPath = ServerActor.serverHashToPathMap

      //if it's server supervisor
      if (serverId == 0) {
        val firstServer = actorSystem.actorSelection(ServerActor.serverHashToPathMap.get(serversHash.head).getOrElse(""))
        //logger.info("firstServer "+firstServer.pathString)
        val future = firstServer ? loadMovie(movie)
        val result = Await.result(future, timeout.duration)
        sender() ! result
      }

      else {
        val temp = new mutable.TreeMap[Int,(Int,String)]()
        val t = new mutable.TreeSet[Data]()(Data)
        val fingerTable = ServerActor.fingerTables.get(currServerHashedValue).getOrElse(temp)
        val movies = ServerActor.movies.get(currServerHashedValue).getOrElse(t)
        val currServersPredecessorHashKey = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._1

        /**
         * if movie's hash key is less than cur-server's hash key, it belongs to our-server.
         */

        if (currServerHashedValue >= movie.hash_key || (currServersPredecessorHashKey > currServerHashedValue && movie.hash_key >currServersPredecessorHashKey) || currServerHashedValue == currServersPredecessorHashKey){
          movies.add(movie)
          sender() ! ("movie: "+movie.movie_name+" with hash key: "+movie.hash_key+" loaded at server: "+serverId)
        } else {
        var closestPredecessors = fingerTable.filter(entry =>
           (entry._2._1 <= movie.hash_key && entry._2._1 > currServerHashedValue))
          //logger.info("currServers successorsHashedValue  "+ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._2)

          /**
           * search for closest predecessor in our finger table and call load movie from it.
           * if no closest predecessor found, this means your successor is the one who should load movie.
           */
         if(closestPredecessors.isEmpty) {
            val successorPath = ServerActor.serverHashToPathMap.get(ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._2).getOrElse("")
           //logger.info("successorPath "+successorPath)
           val successor = actorSystem.actorSelection(successorPath)
            val future = successor ? loadMovie(movie)
            val result = Await.result(future,timeout.duration)
            sender() ! result
          } else {
          val closestPredecessor = actorSystem.actorSelection(closestPredecessors.last._2._2)
            val future = closestPredecessor ? loadMovie(movie)
            val result = Await.result(future,timeout.duration)
            sender() ! result
          }
        }
      }
    }

    case setSuccessor(hash:Int) => {
      val previous = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))
      ServerActor.preNextKeys.put(currServerHashedValue,(previous._1,hash))
    }

    case getSuccesor() =>{
     sender() !
        ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._2
    }

    case setPredecessor(hash:Int) => {

     val previous = ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))
      ServerActor.preNextKeys.put(currServerHashedValue,(hash,previous._2))
    }

    case getPredecessor() =>{
      sender() ! ServerActor.preNextKeys.get(currServerHashedValue).getOrElse((-1,-1))._1
    }


    case getServerSnapshot() =>{

    }

  }

  /**
   * To search movie key if there are many keys. (didn't use this yet.)

   */

  def binarySearch(list:mutable.ListBuffer[Int],hi:Int,lo:Int,key:Int):Int = {
    if (lo == hi)  lo

    val mid:Int = (lo+hi)/2
    if(list(mid) > key) {
      binarySearch(list,mid,lo,key)
    } else {
      binarySearch(list,hi,mid+1,key)
    }

  }

}


object ServerActor {

  val serverActorHashedTreeSet:mutable.TreeSet[Int] = new mutable.TreeSet[Int]()
  val serverHashToPathMap = new mutable.HashMap[Int, String]()
  val fingerTables = new mutable.HashMap[Int,mutable.TreeMap[Int, (Int, String)]]()
  val movies = new mutable.HashMap[Int,mutable.TreeSet[Data]];
  val preNextKeys = new mutable.HashMap[Int,(Int,Int)]
  var serverCount = 0;
  val serverHashesDone = new mutable.HashSet[Int]()

  private val logger = Driver.logger


  //responsible for adding server node
  case class addMeToRing(curServer:ActorRef)
  //responsible for updating keys (i.e movie data) during server addition/removal
  case class updateKeys(list:List[Data],key:Int)
  //responsible for updating fingertable entries during server addition/removal
  case class updateFingerTable()
  //responsible for loading key into server node.
  case class loadMovie(movie:Data)
  //responsible for looking key
  case class lookUpKey(movie:Data)
  case class getServerSnapshot()
  /**
   * additional methods which might be useful for project
   * purpose of these is to store currServersSuccessor and predecessor to make it more look like a circular linked list (of Servers).

   */
  case class setSuccessor(hash:Int)

  case class getSuccesor()

  case class setPredecessor(hash:Int)

  case class getPredecessor()

  //returns total list of server keys
  def getServersHashedSet:mutable.TreeSet[Int] = {
    serverActorHashedTreeSet
  }

  //registers server by adding it's hash and path to respective tables
  def registerServer(hashKey:Int, serverPath:String): Boolean = {
    serverActorHashedTreeSet += hashKey
    serverHashToPathMap += (hashKey -> serverPath)
    fingerTables.addOne(hashKey,new mutable.TreeMap[Int,(Int,String)]())
    movies.addOne(hashKey,mutable.TreeSet[Data]()(Data) )


    true
  }



}