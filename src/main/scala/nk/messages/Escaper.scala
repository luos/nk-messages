package nk.messages


object Escaper {

  case class SafeHtml(text: String)


  def removeHtml(text: String): SafeHtml = {
    import org.jsoup.Jsoup
    import org.jsoup.nodes.Document
    import org.jsoup.safety.Whitelist
    val document = Jsoup.parse(text)
    document.outputSettings(new Document.OutputSettings().prettyPrint(false)) //makes html() preserve linebreaks and spacing
    document.select("br").append("\\n")
    document.select("p").prepend("\\n\\n")
    document.select("div").prepend("\\n")
    val s = document.html.replaceAll("\\\\n", "\n")
    SafeHtml(Jsoup.clean(s, "", Whitelist.none, new Document.OutputSettings().prettyPrint(false)).trim)
  }
}

