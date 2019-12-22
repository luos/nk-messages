package nk.messages.priv.messages

import cats.effect.IO

object MetadataRenderer {

  case class Metadata(
                       title: String,
                       link: Option[String]
                     )

}

trait MetadataRenderer {

  import MetadataRenderer.Metadata

  def render(meta: Option[Map[String, String]]): IO[Option[Metadata]] = IO.pure(None)
}
