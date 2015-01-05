package cakesolutions.signing

import akka.actor.ActorRef
import cakesolutions.logging.{Logging => LoggingActor}
import java.util.UUID
import org.joda.time.{DateTimeZone, DateTime, Seconds}
import scala.concurrent.duration._
import scalaz._
import Scalaz._

// TODO: implement actor using persistence?
trait SignatureServer extends MerkleTrees with LoggingActor with ValidationFunctions {
  this: HashingFunction =>

  import SignatureProtocol._

  private case class Event(receivedAt: DateTime, sender: ActorRef, data: Hash)
  private case class SendTimestamp(period: DateTime)

  // TODO: shard actor based on server and client ID?
  private var certificates = Map.empty[UUID, PublicKeyCertificate]
  private var events = List.empty[Event]
  private var state = State()

  private def valid(certificate: PublicKeyCertificate): ValidationNel[String, Unit] = {
    val validRequest      = if (!certificates.contains(certificate.client.id)) success(()) else failureNel("certificate already exists")
    val validCreationDate = if (certificate.createdAt.isBeforeNow)             success(()) else failureNel("certificate has an invalid creation date")
    val validServer       = if (certificate.server == self.path)               success(()) else failureNel("certificate is bound to a different signature server")

    validRequest +++ validCreationDate +++ validServer
  }

  def receive = external orElse internal

  // Every second, we return aggregated timestamp data to senders of outstanding events (in the given time interval)
  context.system.scheduler.schedule(0.seconds, 1.second) {
    self ! SendTimestamp(new DateTime(DateTimeZone.UTC))
  }

  // Event processing that is external/public to the signature server
  def external: Receive = {
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
        // We store received event hashes - scheduler is "responsible" for adding aggregated events to hash calendar
        events = events :+ Event(new DateTime(DateTimeZone.UTC), sender(), hash((data, client.id, self.path)))
      } else {
        sender() ! -\/(s"Failed to timestamp $data for client $client")
      }
  }

  // Event processing that is internal/private to the signature server
  def internal: Receive = {
    case SendTimestamp(period) =>
      val receivers = events.filter(evt => Seconds.secondsBetween(evt.receivedAt, period).getSeconds == 0)
      events = events.filterNot(evt => Seconds.secondsBetween(evt.receivedAt, period).getSeconds == 0)
      val (timestamp, newState) = append(hash(receivers.map(_.data): _*), state)
      val timestampProof = hashChain(offset, state.roots) // FIXME:
      state = newState
      receivers.foreach(_.sender ! \/-(Timestamp(timestamp, timestampProof)))
  }

}
