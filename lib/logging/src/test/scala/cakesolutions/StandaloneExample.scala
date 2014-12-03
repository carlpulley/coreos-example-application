package cakesolutions

import cakesolutions.logging.Logger

object StandaloneExample extends App with ExceptionLogging {

  val log = Logger(this.getClass)

  log.info("Informational logging message - logging system started")
  log.error(s"Error logging message with exception stack trace: ${exceptionString(new RuntimeException("test exception message"))}")
  log.debug("Debug logging message - shutting logging system down NOW!")

  log.shutdown()
}
