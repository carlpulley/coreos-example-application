# Hello World CoreOS Application

## Building

    sbt clean compile test docker

## Deploying Docker Containers

    docker push carlpulley/helloworld:v0.1.0-SNAPSHOT

## Running on a CoreOS Cluster

    git clone https://github.com/carlpulley/coreos-vagrant.git
    cd coreos-vagrant
    INSTANCE=1,2 CLOUD_CONFIG=helloworld/akka METADATA="akka=true" vagrant up
    INSTANCE=3 CLOUD_CONFIG=helloworld/akka METADATA="akka=true,load-balancer=true" vagrant up
    INSTANCE=4 CLOUD_CONFIG=cassandra METADATA="cassandra=true" vagrant up
    # You should now have a 4 machine cluster provisioned
    ssh-add ~/.vagrant.d/insecure_private_key
    vagrant ssh core-01 -- -A
    fleetctl start app@{1..4} vulcand cassandra seed
    # You should now have:
    #   - a 'Hello World!' Akka cluster running with 4 members
    #   - a Vulcan load balancer interfacing with the cluster
    #   - events being persisted to a Cassandra journal/snapshot store!
    # TERMINAL 1 - execute the following command:
    fleetctl ssh app@1
    docker logs -f app-1 | grep -v akka.cluster.ClusterHeartbeatSender
    # TERMINAL 2 - execute the following shell commands:
    for i in 1 2 3 4; do
      curl -v -H "Host: helloworld.cakesolutions.net" http://172.17.8.103:8888/ping/$i
    done
    # In viewing terminal 1, you should see evidence that:
    #   - Vulcand has round-robin delivered a GET request to app-1
    #   - cluster sharding is used to route messages to the correct 'Hello World' actor endpoint hosting app-1
    #   - evidence of actor spin up and passivation
    #   - each curl request should return the IP address of the 'Hello World' application that handled the request
