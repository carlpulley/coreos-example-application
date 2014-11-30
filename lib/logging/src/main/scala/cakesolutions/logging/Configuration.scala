package cakesolutions.logging

import com.typesafe.config.ConfigFactory

trait Configuration {

  val config = ConfigFactory.load()

}
