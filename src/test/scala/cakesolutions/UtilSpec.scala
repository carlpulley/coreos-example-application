package cakesolutions

import org.specs2.mutable.Specification

class UtilSpec extends Specification with Util {

  "toAddress" should {

    "be able to parse full Akka addresses" in {
      val addr = "akka.tcp://etcd-client@10.42.42.2:53617"

      val result = toAddress(addr)

      result.protocol === "akka.tcp"
      result.system === "etcd-client"
      result.host === Some("10.42.42.2")
      result.port === Some(53617)
    }

  }

}
