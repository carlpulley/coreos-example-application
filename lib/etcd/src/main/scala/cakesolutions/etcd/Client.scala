package cakesolutions.etcd

import akka.actor.ActorSystem
import akka.io.IO
import akka.pattern.ask
import net.nikore.etcd.EtcdExceptions._
import net.nikore.etcd.EtcdJsonProtocol._
import scala.concurrent.Future
import scala.concurrent.duration._
import spray.can.Http
import spray.client.pipelining._
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.json._

class Client(conn: String) {
  private[etcd] val baseUrl = s"$conn/v2/keys"
  private[etcd] implicit val system = ActorSystem("etcd-client")

  import system.dispatcher

  system.registerOnTermination({
    IO(Http).ask(Http.CloseAll)(1.second)
  })

  def getKey(key: String): Future[EtcdResponse] = {
    getKeyAndWait(key, wait = false)
  }

  def getKeyAndWait(key: String, wait: Boolean = true): Future[EtcdResponse] = {
    defaultPipeline(Get(s"$baseUrl/$key?wait=$wait"))
  }

  def setKey(key: String, value: String, ttl: Option[Duration] = None): Future[EtcdResponse] = {
    val ttlOption = ttl.map(t => Map("ttl" -> t.toSeconds.toString)).getOrElse(Map.empty)
    defaultPipeline(Put(Uri(s"$baseUrl/$key").withQuery(Map("value" -> value) ++ ttlOption)))
  }

  def deleteKey(key: String): Future[EtcdResponse] = {
    defaultPipeline(Delete(s"$baseUrl/$key"))
  }

  def createDir(dir: String, ttl: Option[Duration] = None): Future[EtcdResponse] = {
    val ttlOption = ttl.map(t => Map("ttl" -> t.toSeconds.toString)).getOrElse(Map.empty)
    defaultPipeline(Put(Uri(s"$baseUrl/$dir").withQuery(Map("dir" -> "true") ++ ttlOption)))
  }

  def listDir(dir: String, recursive: Boolean = false): Future[EtcdListResponse] = {
    val pipline: HttpRequest => Future[EtcdListResponse] = (
      sendReceive
        ~> mapErrors
        ~> unmarshal[EtcdListResponse]
      )

    pipline(Get(s"$baseUrl/$dir/?recursive=$recursive"))
  }

  def deleteDir(dir: String, recursive: Boolean = false): Future[EtcdResponse] = {
    defaultPipeline(Delete(s"$baseUrl/$dir?recursive=$recursive"))
  }

  private[etcd] val mapErrors = (response: HttpResponse) => {
    if (response.status.isSuccess) response
    else {
      response.entity.asString.parseJson.convertTo[Error] match {
        case e if e.errorCode == 100 => throw KeyNotFoundException(e.message, "not found", e.index)
        case e => throw new RuntimeException("General error: " + e.toString)
      }
    }
  }

  private[etcd] val defaultPipeline: HttpRequest => Future[EtcdResponse] = (
    sendReceive
      ~> mapErrors
      ~> unmarshal[EtcdResponse]
    )
}
