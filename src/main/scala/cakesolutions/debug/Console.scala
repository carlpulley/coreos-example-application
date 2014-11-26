package cakesolutions.debug

import scala.tools.nsc.Settings
import scala.tools.nsc.interpreter.ILoop

private[cakesolutions] object Console {
  def apply() = {
    val settings = new Settings
    settings.usejavacp.value = true
    settings.deprecation.value = true

    new Console().process(settings)
  }
}

private[cakesolutions] class Console extends ILoop {
  override def prompt = "==> "

  override def printWelcome() {
    echo("\n" +
      "         \\,,,/\n" +
      "         (o o)\n" +
      "-----oOOo-(_)-oOOo-----")
  }
}
