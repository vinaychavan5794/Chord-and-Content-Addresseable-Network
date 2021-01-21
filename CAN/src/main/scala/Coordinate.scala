import java.io.Serializable


/**
 * Class Coordinate is a model class used to store the coordinates of a peer
 *
 *
 *
 */
@SerialVersionUID(1L)
class Coordinate extends Serializable {
  /**
   * Shared variables
   */
  var lowerX = .0
  var lowerY = .0
  var upperX = .0
  var upperY = .0
  var centerX = .0
  var centerY = .0

  /**
   * Constructor
   *
   * @param lowerX
   * @param lowerY
   * @param upperX
   * @param upperY
   * @param centerX
   * @param centerY
   */
  def this(lowerX: Double, lowerY: Double, upperX: Double, upperY: Double, centerX: Double, centerY: Double) {
    this()
    this.lowerX = lowerX
    this.lowerY = lowerY
    this.upperX = upperX
    this.upperY = upperY
    this.centerX = centerX
    this.centerY = centerY
  }

  /**
   * Constructor without center coordinates
   *
   * @param lowerX
   * @param lowerY
   * @param upperX
   * @param upperY
   */
  def this(lowerX: Double, lowerY: Double, upperX: Double, upperY: Double) {
    this()
    this.lowerX = lowerX
    this.lowerY = lowerY
    this.upperX = upperX
    this.upperY = upperY
    centerX = (lowerX + upperX) / 2
    centerY = (lowerY + upperY) / 2
  }

  override def toString: String = "\nlowerX:" + lowerX + "\tupperX: " + upperX + "\nlowerY: " + lowerY + "\tupperY: " + upperY + "\n"

  /**
   * contains checks if the point lies in the current coordinate system
   *
   * @param x
   * @param y
   * x and y are points that need to be checked
   * @return
   */
  def contains(x: Double, y: Double): Boolean = {
    if (lowerX <= x && upperX >= x && lowerY <= y && upperY >= y) return true
    false
  }

  /**
   * distance calculates the distance of point to the center of the peer
   *
   * @param x
   * @param y
   * x, y is point that has to be checked
   * @return
   */
  def distance(x: Double, y: Double): Double = Math.sqrt(Math.pow(centerY - y, 2) + Math.pow(centerX - x, 2))

  /**
   * splitVertically checks if the node has to be split peer vertically
   *
   * @return true if answer is yes
   */
  def splitVertically: Boolean = {
    if (upperX - lowerX >= upperY - lowerY) return true
    false
  }

  /**
   * splits the coordinates vertically
   */
  def performSplitVertically(): Unit = {
    upperX = (lowerX + upperX) / 2
    centerX = (lowerX + upperX) / 2
    centerY = (upperY + lowerY) / 2
  }

  /**
   * splits the coordinates horizontally
   */
  def performSplitHorizontally(): Unit = {
    upperY = (lowerY + upperY) / 2
    centerY = (lowerY + upperY) / 2
    centerX = (lowerX + upperX) / 2
  }

  /**
   * checks if given node is a subset in X
   *
   * @return true if x is a subset
   */
  def isSubsetX(c: Coordinate): Boolean = {
    if ((c.lowerX >= lowerX && c.upperX <= upperX) || (c.lowerX <= lowerX && c.upperX >= upperX)) return true
    false
  }

  /**
   * checks if given node is a subset in Y
   *
   * @return true if y is a subset
   */
  def isSubsetY(c: Coordinate): Boolean = {
    if ((c.lowerY >= lowerY && c.upperY <= upperY) || (c.lowerY <= lowerY && c.upperY >= upperY)) return true
    false
  }

  /**
   * checks if given node is a adjacent in X
   *
   * @param c
   * @return
   */
  def isAdjacentX(c: Coordinate): Boolean = {
    if (c.lowerX == upperX || c.upperX == lowerX) return true
    false
  }

  /**
   * checks if given node is a adjacent in Y
   *
   * @param c
   * @return
   */
  def isAdjacentY(c: Coordinate): Boolean = {
    if (c.lowerY == upperY || c.upperY == lowerY) return true
    false
  }

  /**
   * checks if given coordinate is of same size as current
   *
   * @param c
   * @return
   */
  def isSameSize(c: Coordinate): Boolean = {
    if (((upperX - lowerX) == (c.upperX - c.lowerX)) && ((upperY - lowerY) == (c.upperY - c.lowerY))) return true
    false
  }

  /**
   * updates the current coordinates with the given coordinates
   *
   * @param c
   * @return
   */
  def updateCoordinate(c: Coordinate): Coordinate = {
    if (c.lowerX >= upperX) c.lowerX = lowerX
    else if (c.upperX <= lowerX) c.upperX = upperX
    if (c.lowerY >= upperY) c.lowerY = lowerY
    else if (c.upperY <= lowerY) c.upperY = upperY
    c
  }
}
