# Hello World CoreOS Application

## Building

    sbt clean compile test docker

## Deploying Docker Containers

    docker push carlpulley/helloworld:v0.1.0-SNAPSHOT

## Running on a CoreOS Cluster

    git clone https://github.com/carlpulley/coreos-vagrant.git
    cd coreos-vagrant
    curl https://discovery.etcd.io/new
    # Edit `config.rb` with the UUID token obtained above
    CLOUD_CONFIG=helloworld/akka METADATA="type=akka" vagrant up
    # You should now have a 3 machine cluster provisioned
    vagrant ssh core-01 -- -A
    fleetctl start app@1.service app@2.service app@3.service app@4.service
    # You should now have a 'Hello World!' Akka cluster running with 4 members!
