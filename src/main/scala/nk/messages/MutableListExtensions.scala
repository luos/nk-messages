package nk.messages

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

object MutableListExtensions {

  /**
   * To provide backwards compatibility with scala 2.12
   *
   * @param listBuffer
   * @tparam T
   */
  implicit class InPlaceList[T](val listBuffer: mutable.Buffer[T]) extends AnyVal {
    def filterInPlace(fn: (T) => Boolean): mutable.Seq[T] = {
      listBuffer.foreach(elem => {
        if (!fn(elem)) {
          listBuffer -= elem
        }
      })
      listBuffer
    }
  }


}
