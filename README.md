# Description : To implement the  Content Addresseable Network (CAN) algorithm using Akka/HTTP-based simulators (with fault tolerance) and compare the results with the CHORD algorithm


### Project members

* Vinay Indrajit Chavan
* Vignan Linga
* Rohith Venkata Rentachintala
* Varunya Yanamadala
* Sai Teja Karnati

### CAN Overview

* The Content Addressable Network (CAN) is a distributed, decentralized P2P infrastructure that provides hash table functionality on an Internet-like scale. The architectural design is a virtual multi-dimensional Cartesian coordinate space, completely independent of the physical location and physical connectivity of the nodes. Points within the space are identified with coordinates. The entire coordinate space is dynamically partitioned among all the nodes in the system such that every node possesses at least one distinct zone within the overall space.

### Implemention Details

* Bootstrap Class : It is used to connect a new peer to an existing peer in the system

* Client Class : It implements functionality for peers in a CAN system. It has functions to join, leave, view peers, add, search and list files.

* Node joining Process : 
					
	1. Find a node which is already in the Network.
	2. Identify a zone that can be split
	3. Update the routing tables of nodes neighboring the newly split zone.
		
	Bootstrapping nodes are used to inform the joining node of IP addresses of nodes currently in the overlay network. After the joining node receives an IP address of a node already in the CAN, it can attempt to identify a zone for itself. The joining node randomly picks a point in the coordinate space and sends a join request, directed to the random point, to one of the received IP addresses. The nodes already in the overlay network route the join request to the correct device via their zone-to-IP routing tables. Once the node managing the destination point's zone receives the join request, it may honor the join request by splitting its zone in half, allocating itself the first half, and allocating the joining node the second half. If it does not honor the join request, the joining node keeps picking random points in the coordinate space and sending join requests directed to these random points until it successfully joins the network. After the zone split and allocation is complete, the neighboring nodes are updated with the coordinates of the two new zones and the corresponding IP addresses. Routing tables are updated and updates are propagated across the network.
	
* Node Leaving Process: 

	1. Identify the node which is departing
	2. Have the departing node's zone merged or taken over by a neighbouring node
	3. Update the routing tables across the network.


### Test Cases:

* BootstrapBinding: This test case checks if the binding of the Bootstrap server ip with the specified name was successful or not.
* RandomCoordinateCheck : This test case checks if the random coordinate generator function returns a double which is not null.
* HostAddressCheck: This test case checks if the Host IP Address is not null.
* ClientIP: This test case matches the client ip with the specified ip.
* ClientNull: This test case checks if the client object created is initialized properly.

### Output

* After adding a node to the CAN :
```
Dec 9, 2020 3:15:01 PM Client join
INFO: First Node in CAN
Dec 9, 2020 3:15:01 PM Client join
INFO: Joined Successfully

```
* After loading File (Data) to a node:
```
Dec 9, 2020 3:15:01 PM Client addFile
INFO: Adding file Test on 151501

```

* Look up for a File(Data):
```
Dec 9, 2020 3:15:01 PM Client searchFile
INFO: File found on 192.168.1.6 with route: 151501

```

## Cluster sharding
We tried to integrate cluster sharding with the Chord project. We got the cluster sharding working but only for one node. We couldn't integrate it for the rest of the Chord Nodes. 

Follow commands to run the CHORD-Sharding:

here proj = Chord-Cluster Sharding

* 1 proj>sbt

* 2 sbt:proj>run                                               -   (to run the chord )

* 3 sbt:proj>runMain Sharding.ShardingMovies                   -   (to Run shardnig on single node)


you will see this actor shards being created:

* Server online at http:/localhost:8090/

* press Return to Stop...

## AWS EC2 and Docker  of project
AWS EC2 Demo can be found using the following link. 
https://www.youtube.com/watch?v=SMAziAC9Hl8&feature=youtu.be&ab_channel=Groove

Docker image can be pulled using this link.
docker pull groove007/cs441newimage:latest

You can run the image using this:
docker run groove007/cs441newimage:latest


### Chord Overview

* Chord is a protocol and algorithm for a peer-to-peer distributed hash table. A distributed hash table stores key-value pairs by assigning keys to different computers (known as "nodes"); a node will store the values for all the keys for which it is responsible.

### Functionality

* Adding a node to the ring

    -Following operations are performed when a new node is to be added:
		1. Initialize the new node (the predecessor and the finger table).
		2. Notify other nodes to update their predecessors and finger tables.
		3. The new node takes over its responsible keys from its successor.
		
	Please refer ServerActor class for more details.
	
* Search movie data in some node
    - `ServerActor`
        * This class represents an Akka actor which corresponds to a server node in the chord ring.
        * The messages defined on this actor are as follows
            * `addMeToRing`
                * This case class is responsible for adding a server node in the ring.
            * `updateKeys`
                * This case class is responsible for updating keys (i.e movie data) during server addition/removal.
            * `updateFingerTable`
                * This case class is responsible for updating fingertable entries during server addition/removal
			* `loadMovie`
				* This case class is responsible for loading key i.e. movie data into server node.
			* `lookUpKey`
				* This case class is responsible for Looking up for a key i.e. movie data.
    - `UserActor`
        * This actor corresponds to the user whose functions include adding a unit of data and the looking it up.
        * `lookMovie`
            * This case class is responsible to search for a movie data from the Chord ring.
        * `addUser`
            * This case class is responsible for initialization of a particular instance of a user actor.
        * `loadMovie`
            * This case class is responsible to load movie data on a server node.
			
* Loading movie data to a server node in the ring
    * This operation adds a unit of data which corresponds to a movie. The process starts from the first server in the chord.
    First server looks up its finger table to find the server node which is the closest predecessor to the hashed movie data. 
    If there a predecessor found,then the same process is repeated for the next closest predecessor node. 
    When there is no closest predecessor found, the movie data is loaded in the successor node.   
  
	
* Lookup for a data in some node 
    * This operation adds a unit of data which corresponds to a movie. The process starts from the first server in the chord.
        First server looks up its finger table to find the server node which is the closest predecessor to the hashed movie data. 
        If there a predecessor found,then the same process is repeated for the next closest predecessor node. 
        When there is no closest predecessor found, the movie data is looked up in the successor node and movie data 
        is returned back to the user.
         
	
* `HashUtils`

    *  We made use of this class from Link : https://github.com/AdityaSa1t/Evaluating-CHORD-Algorithm-Using-an-Akka-based-Simulator

### Test Cases:

* Driver.getMovies: This test case checks if the movies list is unique and correctly read from the resources.
* Utils.hash : This test case hashes the given input string and maintains uniqueness.
* Server add test: This test case checks if the node was successfully added to the chord ring.
* load and lookup success test: This test case checks if the movie data was successfully loaded and looked up.
* load and lookup fail test: This test case checks if the resultant movie data does not exist when a movie data 
    is looked up which is not present in the chord ring.



### Output

* After adding a server node to the ring , we get the following data :
```
Nov 18, 2020 2:54:55 PM Master$$anonfun$receive$1 applyOrElse
INFO: Master - adding new server 1 with hash key 886 at akka://AkkaHW3/user/server-actor-1
Nov 18, 2020 2:54:55 PM Driver$ main
INFO: result :server: 1 added succesfully with hashkey: 886

```
* After loading data to a node ,we get the following data :
```
INFO: loading movie Twilight: Breaking Dawn with hash 704
Nov 18, 2020 4:11:57 PM Driver$ main
INFO: result6 :movie: Twilight: Breaking Dawn with hash key: 704 loaded at server: 3
Nov 18, 2020 4:11:57 PM Driver$ main

```

* Look up for a movie data:
```
INFO: loading movie Twilight: Breaking Dawn with hash 704
Nov 18, 2020 4:11:57 PM Driver$ main
INFO: result6 :movie: Twilight: Breaking Dawn with hash key: 704 loaded at server: 3
Nov 18, 2020 4:11:57 PM Driver$ main

INFO: looking for movie 869
Nov 18, 2020 4:11:57 PM Driver$ main
INFO: result7 :movie: Twilight doesn't exist
Nov 18, 2020 4:11:57 PM Driver$ main

```

* Get Snapshot:
```
INFO: **************** SNAPSHOT ***************
Nov 18, 2020 5:45:43 PM Master$$anonfun$receive$1 applyOrElse
INFO: order of servers in chord with their hashkeys and list of movies it's holding

 actor /server-actor-2 hashkey 19 movies Twilight: Breaking Dawn, When in Rome, What Happens in Vegas, 
 actor /server-actor-3 hashkey 166 movies 
 actor /server-actor-4 hashkey 239 movies 
 actor /server-actor-1 hashkey 528 movies 
```

### Improvements to Chord: 
- Integration of Chord Simulator Driver with a statistical package called R for sampling probabilistic distributions which is primarily used for lookup of data items stored on the nodes.
  Monte Carlo Simulation was simulated with normal distribution over the number of movies present in the data.csv file with specified mean and standard deviation and replicated over 4 runs 
  and sampled with specified number of samples and with no replacement. The values of mean, standard deviation and number of samples & number of replication runs are fetched from configuration
  
  -- Total number of values present in data.csv files = 72
  
  -- Mean = 50
  
  -- Standard Deviation = 7
  
  -- Number of Samples = 5
  
  -- Number of Replication Runs = 4




### Installation Instructions

This section contains the instructions on how to run the simulations implemented as part of this homework, the recommended procedure is to use IntellJ IDEA with the Scala plugin installed.

1. Open a terminal of your choice, and navigate to the disk location of where you want to clone this repo.
2. Type git clone https://rrenta2@bitbucket.org/cs441-fall2020/overlaynetworksimulator_courseproject_group3.git and execute this command.
3. Open IntellJ IDEA, a welcome screen will be shown, select "Open Project" and navigate to the disk location where this repo was previously cloned (as indicated above in Step 2)
4. Select the Project among the list (CAN/Chord-ClusterSharding/HW3_Chord) and Click on Ok
4. The project now will be setup and built using the SBT structure
6. You may now go to src/main/scala/ and run the Driver class in the respective projects. A run configuration is automatically created when you click the green arrow next to the main method of the Driver class.

Note: To execute CAN, navigate to src/main/scala folder and run the Bootstrap class first.The output will be an ip address. Provide this ip address as a command line argument in the run configuration of the Client class. Now execute the Driver class.