package cakesolutions

package signing

import java.security.{SecureRandom, MessageDigest}
import scala.pickling._

trait HashingFunction extends Configuration {

  import SignatureProtocol.Hash

  private val randomGen = SecureRandom.getInstance(config.getString("signing.algorithm.random"))
  val hashingId = config.getString("signing.algorithm.hash")

  def hash(data: Array[Byte]): Hash = {
    Hash(MessageDigest.getInstance(hashingId).digest(data), hashingId)
  }

  def hash[D](data: D*)(implicit format: PickleFormat): Hash = {
    hash(data.pickle)
  }

  val zero: Hash = hash(Array[Byte](0))

  def random: Hash = {
    val size = MessageDigest.getInstance(hashingId).getDigestLength
    hash(randomGen.nextBytes(new Array[Byte](size)))
  }

}
