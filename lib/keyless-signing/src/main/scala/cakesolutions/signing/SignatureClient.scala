package cakesolutions

package signing

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import cakesolutions.logging.{Logging => LoggingActor}
import java.util.concurrent.TimeUnit
import org.joda.time.{DateTimeZone, DateTime, Seconds}
import scala.concurrent.duration._
import scala.concurrent.Future
import scalaz._
import Scalaz._

trait SignatureClient extends MerkleTrees with LoggingActor with Configuration {
  this: HashingFunction =>

  import SignatureProtocol._

  import context.dispatcher

  implicit val timeout = Timeout(config.getDuration("signing.timeout", TimeUnit.SECONDS).seconds)

  /**
   * Reference to signature server actor.
   */
  def signatureServer: ActorRef

  // TODO: implement ideas of "Almost Optimal Hash Sequence Traversal" by D.Coppersmith and M.Jakobsson
  // NOTE: here we memoize our hash sequence traversal function for efficiency (i.e. we trade time for space)
  private val hashTraversal = Memo.immutableHashMapMemo[(Int, Hash), (List[Hash], List[MerkleTree])] {
    case (lifetime: Int, secret: Hash) =>
      val keyHashChain = (1 to lifetime).foldRight(List(secret)) {
        case (_, hashChain) =>
          hash(hashChain.last) +: hashChain
      }
      val dataTree = keyHashChain.zipWithIndex.map {
        case (h, i) if i != 0 && i % 2 == 0 =>
          leaf(TreeNode(hash(h), 0))

        case (h, _) =>
          leaf(TreeNode(h, 0))
      }

      (keyHashChain, dataTree)
  }

  /**
   * Generates and publishes a public key certificate for a client.
   *
   * @param client   client that the public key is to be published for
   * @param lifetime lifetime for public key (in seconds)
   * @param secret   secret key (randomly generated and known to client only)
   * @return         (published) public key certificate
   */
  def publishPublicKey(client: ClientId)(implicit lifetime: Int, secret: Hash): Future[\/[String, PublicKeyCertificate]] = {
    require(lifetime > 0, "Key lifetime needs to be strictly positive")

    val (keyHashChain, dataTree) = hashTraversal(lifetime, secret)
    val root = reduce(balance(dataTree))
    val cert = PublicKeyCertificate(client, PublicKey(keyHashChain(0), root.rootLabel.hash), new DateTime(DateTimeZone.UTC), signatureServer.path)

    (signatureServer ? Publish(cert)).mapTo[\/[String, Unit]].map(_.map(_ => cert))
  }

  /**
   * Revokes a previously published public key.
   *
   * @param certificate public key certificate that is to be revoked (must be previously published)
   * @param lifetime    lifetime for public key (in seconds)
   * @param secret      secret hash used to generate public key (known to client only)
   * @return
   */
  def revokePublicKey(certificate: PublicKeyCertificate)(implicit lifetime: Int, secret: Hash): Future[\/[String, Unit]] = {
    val offset = Seconds.secondsBetween(certificate.createdAt, new DateTime(DateTimeZone.UTC))
    val (keyHashChain, dataTree) = hashTraversal(lifetime, secret)
    val authProof = hashChain(offset.getSeconds, dataTree)
    val auth = keyHashChain(offset.getSeconds)

    (signatureServer ? Revoke(certificate, RevocationNote(auth, authProof))).mapTo[\/[String, Unit]]
  }

  /**
   * Used to sign client data. As the signature server encapsulates a hash calendar with a 1 second accuracy, this operation
   * generates timestamps in time that is bounded by this limit.
   *
   * @param rawData     data to be signed
   * @param certificate public key certificate to be used in signing (must already be published)
   * @param lifetime    lifetime of public key (in seconds)
   * @param secret      secret hash used to generate public key (known to client only)
   * @return            signature of the signed data
   */
  def signData[D](rawData: D, certificate: PublicKeyCertificate)(implicit lifetime: Int, secret: Hash): Future[\/[String, Signature]] = {
    val offset = Seconds.secondsBetween(certificate.createdAt, new DateTime(DateTimeZone.UTC))
    val (keyHashChain, dataTree) = hashTraversal(lifetime, secret)
    val authProof = hashChain(offset.getSeconds, dataTree)
    val auth = keyHashChain(offset.getSeconds)
    val dataHash = hash(rawData)
    val data = hash(dataHash, auth)

    (signatureServer ? GetTimestamp(data, certificate.client)).mapTo[\/[String, Timestamp]].map {
      case \/-(timestamp) =>
        \/-(Signature(certificate.client, offset, auth, authProof, timestamp))

      case err @ -\/(_) =>
        err
    }
  }

  /**
   * Used to verify a signature against a public key.
   *
   * @param signature   signature to be verified
   * @param certificate public key certificate to be used in verification
   * @return            unit value indicating that verification was successful
   */
  def verifySignature(signature: Signature, certificate: PublicKeyCertificate): ValidationNel[String, Unit] = {
    val validClientId  = if (signature.client.id == certificate.client.id)                                success(()) else failureNel("client IDs fail to match")
    val validRootHash  = if (rootHash(signature.auth, signature.authProof) == certificate.publicKey.root) success(()) else failureNel("signature fails to compute certificate root hash")
    val timestamp      = signature.timestamp.dataProof.zipWithIndex.map {
      case (Left(_, _), height) =>
        math.pow(2, height - 1).toInt

      case (Right(_, _), _) =>
        0
    }.sum
    val validTimestamp = if (timestamp == certificate.createdAt.plus(signature.offset).getMillis / 1000)  success(()) else failureNel("signature has an incorrect timestamp")
    val validServerId  = if (signature.timestamp.server == certificate.server)                            success(()) else failureNel("signature is bound to a different server")

    validClientId +++ validRootHash +++ validTimestamp +++ validServerId
  }

}
