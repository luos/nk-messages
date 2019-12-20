package nk.messages

import java.time.{Instant, LocalDateTime}

import cats.effect.IO

object TestUtils {
  def assertHappenedNowInstant(date: Instant): Symbol = {
    assertHappenedNowInstant(Some(date))
  }

  def assertHappenedNowInstant(dateOpt: Option[Instant]): Symbol = {
    val date = dateOpt.get
    val d = Instant.now().minusSeconds(15)
    if (d.isBefore(date)) {
      Symbol("ok")
    } else {
      throw new Exception(s"Failed asserting that date $date is earlier than $d.")
    }
  }

  implicit class IoResult[T](val io: IO[T]) extends AnyVal {
    def run = io.unsafeRunSync()
  }

}
