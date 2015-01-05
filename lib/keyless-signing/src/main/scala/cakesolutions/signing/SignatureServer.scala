package cakesolutions.signing

import cakesolutions.logging.{Logging => LoggingActor}
import java.util.UUID
import scalaz._
import Scalaz._

// TODO: implement actor using persistence?
trait SignatureServer extends MerkleTrees with LoggingActor with ValidationFunctions {
  this: HashingFunction =>

  import SignatureProtocol._

  // TODO: shard actor based on client ID?
  private var certificates = Map.empty[UUID, PublicKeyCertificate]

  private var state = State()

  private def valid(certificate: PublicKeyCertificate): ValidationNel[String, Unit] = {
    val validRequest      = if (!certificates.contains(certificate.client.id)) success(()) else failureNel("certificate already exists")
    val validCreationDate = if (certificate.createdAt.isBeforeNow)             success(()) else failureNel("certificate has an invalid creation date")
    val validServer       = if (certificate.server == self.path)               success(()) else failureNel("certificate is bound to a different signature server")

    validRequest +++ validCreationDate +++ validServer
  }

  def receive = {
    case Publish(certificate) =>
      valid(certificate) match {
        case Success(_) =>
          log.info(s"Successfully published certificate $certificate")
          certificates += (certificate.client.id -> certificate)
          sender() ! \/-(())

        case Failure(err) =>
          sender() ! -\/(s"Failed to publish certificate: ${err.list.mkString("; ")}")
      }

    case Revoke(certificate, RevocationNote(auth, hashChain)) =>
      valid(certificate) match {
        case Success(_) =>
          if (rootHash(auth, hashChain) == certificate.publicKey.root) {
            log.info(s"Successfully revoked certificate $certificate")
            certificates -= certificate.client.id
            sender() ! \/-(())
          } else {
            sender() ! -\/("Failed to revoke certificate: revocation note is invalid")
          }

        case Failure(err) =>
          sender() ! -\/(s"Failed to revoke certificate: ${err.list.mkString("; ")}")
      }

    case GetTimestamp(data, client) =>
      if (certificates.contains(client.id)) {
        // FIXME: need to ensure 1 sec. rounds are implemented here!
        //val offset = Seconds.secondsBetween(certificates(client.id).createdAt, new DateTime(DateTimeZone.UTC))
        val (timestamp, newState) = append((data, client.id, self.path), state)
        val timestampProof = ???
        state = newState

        sender() ! \/-(Timestamp(timestamp, timestampProof))
      } else {
        sender() ! -\/(s"Failed to timestamp $data for client $client")
      }
  }

}
