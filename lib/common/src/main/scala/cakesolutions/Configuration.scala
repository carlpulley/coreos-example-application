package cakesolutions

import com.typesafe.config.ConfigFactory

trait Configuration {

  lazy val config = ConfigFactory.load()

}
