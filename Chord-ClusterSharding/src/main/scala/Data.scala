
class Data(Db_key: Int, movieName: String){
  val movie_Db_Key = Db_key
  val hash_key = Db_key
  val movie_name = movieName
}

object Data extends Ordering[Data] {
  override def compare(x: Data, y: Data): Int = x.movie_Db_Key - y.movie_Db_Key
}