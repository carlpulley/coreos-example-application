# Hello World CoreOS Application

## Building

    sbt clean compile test docker

## Deploying Docker Containers

    docker push carlpulley/helloworld:v0.1.0-SNAPSHOT

## Running on a CoreOS Cluster

    git clone https://github.com/carlpulley/coreos-vagrant.git
    cd coreos-vagrant
    curl https://discovery.etcd.io/new
    INSTANCE=1,2 CLOUD_CONFIG=helloworld/akka METADATA="akka=true" vagrant up
    INSTANCE=3 CLOUD_CONFIG=helloworld/akka METADATA="akka=true,load-balancer=true" vagrant up
    INSTANCE=4 CLOUD_CONFIG=cassandra METADATA="cassandra=true" vagrant up
    # You should now have a 4 machine cluster provisioned
    ssh-add ~/.vagrant.d/insecure_private_key
    vagrant ssh core-01 -- -A
    fleetctl start cassandra@1 app@{1..4} vulcand
    # Determine potential seed nodes
    etcdctl ls /akka.cluster.nodes
    # Now form a cluster by specifying a **single** initial seed node
    fleetctl start seed@10.42.42.1:12345
    # You should now have:
    #   - a 'Hello World!' Akka cluster running with 4 members
    #   - a Vulcan load balancer interfacing with the cluster
    #   - events being persisted to a Cassandra journal/snapshot store!
