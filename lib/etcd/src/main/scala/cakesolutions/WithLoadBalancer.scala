package cakesolutions

import cakesolutions.etcd.{Client => EtcdClient, WithEtcd}
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

  case class LoadBalance(domain: String)(implicit etcd: EtcdClient) {

    private var upstreams = Map.empty[String, Seq[Endpoint]]
    private var locations = Map.empty[MicroService, Location]

    def +(mapping: (MicroService, Location)): LoadBalance = {
      val (microservice, location) = mapping

      locations = locations.updated(microservice, location)
      etcd.setKey(s"/vulcand/hosts/$domain/locations/${microservice.name}/path", location.path)
      etcd.setKey(s"/vulcand/hosts/$domain/locations/${microservice.name}/upstream", location.upstream)
      this
    }

    def ++(mapping: (String, Endpoint)): LoadBalance = {
      val (upstreamId, endpoint) = mapping

      upstreams = upstreams.updated(upstreamId, upstreams.getOrElse(upstreamId, Seq.empty[Endpoint]) :+ endpoint)
      etcd.setKey(s"/vulcand/upstreams/$upstreamId/endpoints/${endpoint.id}", endpoint.url)
      this
    }

    def --(mapping: (String, Endpoint)): LoadBalance = {
      val (upstreamId, endpoint) = mapping
      val resolvedEndpoint = upstreams.getOrElse(upstreamId, Seq.empty[Endpoint]).filter(_.url == endpoint.url)

      upstreams = upstreams.updated(upstreamId, upstreams.getOrElse(upstreamId, Seq.empty[Endpoint]).filterNot(_.url == endpoint.url))
      resolvedEndpoint.foreach(e => etcd.deleteKey(s"/vulcand/upstreams/$upstreamId/endpoints/${e.id}"))
      this
    }

  }

}
