package cakesolutions

import cakesolutions.etcd.{Client => EtcdClient, WithEtcd}
import cakesolutions.logging.Logger
import java.util.UUID

trait WithLoadBalancer {
  this: WithEtcd with Configuration =>

  import WithLoadBalancer._

  def balancer: LoadBalance

}

object WithLoadBalancer {

  case class MicroService(name: String)
  case class Endpoint(url: String, id: UUID = UUID.randomUUID())
  case class Location(path: String, upstream: String)

  case class LoadBalance(domain: String)(implicit val etcd: EtcdClient) {

    protected var upstreams = Map.empty[String, Seq[Endpoint]]
    protected var locations = Map.empty[MicroService, Location]
    protected val log = Logger(this.getClass)

    def +(mapping: (MicroService, Location)): LoadBalance = {
      val (microservice, location) = mapping
      log.debug(s"Adding $microservice -> $location location to load balancer domain $domain")

      locations = locations.updated(microservice, location)
      etcd.createDir(s"vulcand/hosts/$domain/locations/${microservice.name}")
      etcd.setKey(s"vulcand/hosts/$domain/locations/${microservice.name}/path", location.path)
      etcd.setKey(s"vulcand/hosts/$domain/locations/${microservice.name}/upstream", location.upstream)
      this
    }

    def ++(mapping: (String, Endpoint)): LoadBalance = {
      val (upstreamId, endpoint) = mapping
      log.debug(s"Adding $upstreamId -> $endpoint endpoint to load balancer domain $domain")

      upstreams = upstreams.updated(upstreamId, upstreams.getOrElse(upstreamId, Seq.empty[Endpoint]) :+ endpoint)
      etcd.createDir(s"vulcand/upstreams/$upstreamId/endpoints")
      etcd.setKey(s"vulcand/upstreams/$upstreamId/endpoints/${endpoint.id}", endpoint.url)
      this
    }

    def --(mapping: (String, Endpoint)): LoadBalance = {
      val (upstreamId, endpoint) = mapping
      val resolvedEndpoint = upstreams.getOrElse(upstreamId, Seq.empty[Endpoint]).filter(_.url == endpoint.url)
      log.debug(s"Removing $upstreamId -> $resolvedEndpoint endpoint from load balancer domain $domain")

      upstreams = upstreams.updated(upstreamId, upstreams.getOrElse(upstreamId, Seq.empty[Endpoint]).filterNot(_.url == endpoint.url))
      resolvedEndpoint.foreach(e => etcd.deleteKey(s"vulcand/upstreams/$upstreamId/endpoints/${e.id}"))
      this
    }

  }

}
