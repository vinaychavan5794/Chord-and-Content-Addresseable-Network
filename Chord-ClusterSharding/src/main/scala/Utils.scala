object Utils {

  def hash (uniqueId: String, numBits: Int) = {
    val md = java.security.MessageDigest.getInstance("SHA-1")
                          .digest(uniqueId.getBytes("UTF-8"))
                          .map("%02x".format(_)).mkString
    val bin = BigInt(md, 16).toString(2).take(numBits)
    Integer.parseInt(bin, 2).toString
  }

}
