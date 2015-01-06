package cakesolutions.signing

import akka.actor.ActorSystem
import org.scalatest._

class HashingFunctionSpec extends WordSpec with HashingFunction {

  val system = ActorSystem("TestSystem")

  def hexToBytes(hex: String): List[Byte] = hex.sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte).toList

  val emptySHA256 = hexToBytes("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")

  "SHA2-256 Cryptographic Hash" when {

    "given an empty string" should {
      "calculate correct hash" in {
        assert(hash("").value.toList == emptySHA256)
        assert(hash("").`type` == "SHA-256")
      }

      "same hash as zero" in {
        assert(hash("").value.toList == zero.value.toList)
        assert(hash("").`type` == "SHA-256")
      }
    }

    "given an empty byte array" should {
      "calculate correct hash" in {
        assert(hash(new Array[Byte](0)).value.toList == emptySHA256)
        assert(hash(new Array[Byte](0)).`type` == "SHA-256")
      }

      "same hash as zero" in {
        assert(hash(new Array[Byte](0)).value.toList == zero.value.toList)
        assert(hash(new Array[Byte](0)).`type` == "SHA-256")
      }
    }

    "given single string argument" should {
      "calculate correct hash for \"one\"" in {
        assert(hash("one").value.toList == hexToBytes("7692c3ad3540bb803c020b3aee66cd8887123234ea0c6e7143c0add73ff431ed"))
        assert(hash("one").`type` == "SHA-256")
      }
    }

    "given string tuple" should {
      "calculate correct hash for \"one\" and \"two\"" in {
        assert(hash("one", "two").value.toList == hexToBytes("5b943976d589db7ad78430e424770733f6505cae4b7e7b60aaa0d0028334a479"))
        assert(hash("one", "two").`type` == "SHA-256")
      }
    }

    "given single integer argument" should {
      "calculate correct hash for 1" in {
        assert(hash(1).value.toList == hexToBytes("4b91447d058246efab2b2a6e7140c2766b5aa42e8482afc46da9b94f7cd92544"))
        assert(hash(1).`type` == "SHA-256")
      }
    }

    "given integer tuple" should {
      "calculate correct hash for 1 and 2" in {
        assert(hash(1, 2).value.toList == hexToBytes("5cde7fd4d4fd4a76d95d56f38d8f5f3708c89b41ceecd5f87263f3e29bf64164"))
        assert(hash(1, 2).`type` == "SHA-256")
      }
    }

  }

}
