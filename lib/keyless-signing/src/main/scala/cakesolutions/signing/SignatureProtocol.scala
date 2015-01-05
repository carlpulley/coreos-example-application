package cakesolutions.signing

import akka.actor.ActorPath
import java.util.UUID
import org.joda.time.{DateTime, Seconds}
import scalaz._

object SignatureProtocol extends ValidationFunctions {

  // We always store hashing algorithm type with the hash value
  case class Hash(value: Array[Byte], `type`: String)
  case class TreeNode(hash: Hash, level: Int)
  type MerkleTree = Tree[TreeNode]

  /**
   * Used to define hash chains in authentication "proofs"
   */
  sealed trait Direction {
    def level: Int
  }
  case class Left private[signing](child: Hash, level: Int) extends Direction {
    require(level >= 0, "tree depth or level must be positive")
  }
  case class Right private[signing](child: Hash, level: Int) extends Direction {
    require(level >= 0, "tree depth or level must be positive")
  }


  /**
   * @param name user/human/logging friendly name for the client (not necessarily unique)
   * @param id   unique client identifier (essentially identifies this client)
   */
  case class ClientId(name: String = "", id: UUID = UUID.randomUUID())

  /**
   * Core data structures used by signature client and server
   */
  case class PublicKey(seed: Hash, root: Hash)
  case class PublicKeyCertificate(client: ClientId, publicKey: PublicKey, createdAt: DateTime, server: ActorPath)
  case class RevocationNote(auth: Hash, authProof: List[Direction])
  case class Signature(client: ClientId, offset: Seconds, auth: Hash, authProof: List[Direction], timestamp: Timestamp)

  /**
   * Signature server (actor) messages
   */
  case class Publish(certificate: PublicKeyCertificate)
  case class Revoke(certificate: PublicKeyCertificate, note: RevocationNote)
  case class GetTimestamp(data: Hash, client: ClientId)
  case class Timestamp(data: Hash, dataProof: List[Direction])

}
