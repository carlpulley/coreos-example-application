package cakesolutions

package signing

import java.io.{ByteArrayOutputStream, ObjectOutputStream}
import java.security.{SecureRandom, MessageDigest}

trait HashingFunction extends Configuration {

  import SignatureProtocol.Hash

  private val randomGen = SecureRandom.getInstance(config.getString("signing.algorithm.random"))
  val hashingId = config.getString("signing.algorithm.hash")

  def hash(bytes: Array[Byte]): Hash = {
    Hash(MessageDigest.getInstance(hashingId).digest(bytes), hashingId)
  }

  def hash[D](data: D*): Hash = {
    val byteStream = new ByteArrayOutputStream(1024)
    val objectStream = new ObjectOutputStream(byteStream)
    if (data.isEmpty) {
      zero
    } else if (data.length == 1) {
      data.head match {
        case obj: String =>
          hash(obj.getBytes("UTF-8"))

        case obj =>
          objectStream.writeObject(obj)
          hash(byteStream.toByteArray)
      }
    } else {
      objectStream.writeObject(data)
      hash(byteStream.toByteArray)
    }
  }

  lazy val zero: Hash = hash(Array[Byte](0))

  def random: Hash = {
    val size = MessageDigest.getInstance(hashingId).getDigestLength
    val randomHash = new Array[Byte](size)
    randomGen.nextBytes(randomHash)
    hash(randomHash)
  }

}
