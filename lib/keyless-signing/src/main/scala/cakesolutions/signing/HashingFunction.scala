package cakesolutions

package signing

import java.security.{SecureRandom, MessageDigest}
import akka.actor.ActorSystem
import akka.serialization.SerializationExtension

trait HashingFunction extends Configuration {

  import SignatureProtocol.Hash

  def system: ActorSystem

  private val randomGen = SecureRandom.getInstance(config.getString("signing.algorithm.random"))
  val hashingId = config.getString("signing.algorithm.hash")
  val serialization = SerializationExtension(system)

  def hash(data: Array[Byte]): Hash = {
    Hash(MessageDigest.getInstance(hashingId).digest(data), hashingId)
  }

  def hash[D](data: D*): Hash = {
    val serializer = serialization.findSerializerFor(data)
    hash(serializer.toBinary(data))
  }

  val zero: Hash = hash(Array[Byte](0))

  def random: Hash = {
    val size = MessageDigest.getInstance(hashingId).getDigestLength
    val randomHash = new Array[Byte](size)
    randomGen.nextBytes(randomHash)
    hash(randomHash)
  }

}
