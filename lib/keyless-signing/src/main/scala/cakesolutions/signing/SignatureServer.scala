package cakesolutions.signing


import cakesolutions.logging.{Logging => LoggingActor}
import java.util.UUID
import org.joda.time.{DateTimeZone, DateTime, Seconds}
import scalaz._
import scalaz.syntax.applicative._

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

    validRequest |@| validCreationDate |@| validServer
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

    case Timestamp(data, client) =>
      if (certificates.contains(client.id)) {
        val offset = Seconds.secondsBetween(certificates(client.id).createdAt, new DateTime(DateTimeZone.UTC))
        val timestamp = ??? // FIXME: hash-tree timestamp for (data, clientId)

        if (???) {
          sender() ! \/-(timestamp)
        } else {
          sender() ! -\/(s"Failed to timestamp $data for client $client: ???")
        }
      } else {
        sender() ! -\/(s"Failed to timestamp $data for client $client")
      }
  }

}
