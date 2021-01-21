import java.io.Serializable


/**
 * Class Neighbour is a model class that stores a neighbour for a peer
 *
 *
 *
 */
@SerialVersionUID(1L)
class Neighbour()

/**
 * Constructor
 */
  extends Serializable {
  /**
   * Global shared variables
   */
  var c: Coordinate = null
  var ip: String = null
  var identifier: String = null

  /**
   * Constructor
   *
   * @param ip
   * @param c
   */
  def this(ip: String, c: Coordinate) {
    this()
    this.ip = ip
    this.c = c
  }

  /**
   * Constructor
   *
   * @param ip
   * @param identifier
   * @param c
   *
   */
  def this(ip: String, identifier: String, c: Coordinate) {
    this()
    this.ip = ip
    this.identifier = identifier
    this.c = c
  }

  override def toString: String = "IP: " + ip + "\nIdentifier: " + identifier + "\nCoordinate: " + c

  override def equals(n: Any): Boolean = {
    if (n.isInstanceOf[Neighbour]) return ip == (n.asInstanceOf[Neighbour]).ip
    false
  }

  override def hashCode: Int = ip.hashCode
}