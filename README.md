# Lift CoreOS Application

## Building

    sbt clean compile test docker

## Deploying Docker Containers

    docker push carlpulley/lift-exercise:v0.1.0-SNAPSHOT
    docker push carlpulley/lift-profile:v0.1.0-SNAPSHOT
    docker push carlpulley/lift-notification:v0.1.0-SNAPSHOT

## Running on a CoreOS Cluster

    git clone https://github.com/carlpulley/coreos-vagrant.git
    cd coreos-vagrant
    INSTANCE=1,2 CLOUD_CONFIG=lift/akka METADATA="lift=true" vagrant up
    INSTANCE=3 CLOUD_CONFIG=lift/akk METADATA="lift=true,load-balancer=true" vagrant up
    INSTANCE=4 CLOUD_CONFIG=cassandra METADATA="cassandra=true" vagrant up
    # You should now have a 4 machine cluster provisioned
    ssh-add ~/.vagrant.d/insecure_private_key
    vagrant ssh core-01 -- -A
    fleetctl start exercise@{1..2} notification@1 profile@1 vulcand cassandra seed
    # You should now have:
    #   - a Lift Akka cluster running with 4 members (2 exercise, 1 notification and 1 profile microservice)
    #   - a Vulcan load balancer interfacing with the cluster
    #   - events being persisted to a Cassandra journal/snapshot store!
    # TERMINAL 1 - execute the following command:
    fleetctl ssh exercise@1
    docker logs -f exercise-1 | grep -v akka.cluster.ClusterHeartbeatSender
    # TERMINAL 2 - execute the following shell commands:
    for i in 1 2 3 4; do
      curl -v -H "Host: lift.cakesolutions.net" http://172.17.8.103:8888/exercise/$i
    done
    # In viewing terminal 1, you should see evidence that:
    #   - Vulcand has round-robin delivered a GET request to exercise-1
    #   - cluster sharding is used to route messages to the correct Lift actor endpoint hosting exercise-1
    #   - evidence of actor spin up and passivation
