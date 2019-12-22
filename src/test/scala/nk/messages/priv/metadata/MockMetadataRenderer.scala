package nk.messages.priv.metadata

import cats.effect.IO
import nk.messages.priv.messages.MetadataRenderer
import nk.messages.priv.messages.MetadataRenderer.Metadata

class MockMetadataRenderer extends MetadataRenderer {
  var result: Option[Metadata] = None

  override def render(meta: Option[Map[String, String]]): IO[Option[Metadata]] = {
    IO.pure(result)
  }

}
