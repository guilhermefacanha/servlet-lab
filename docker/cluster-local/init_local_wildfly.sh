#!/bin/bash

NODE1_PATH="/Users/guilhermelimafacanha/workspace/servers/wildfly-node1"
NODE2_PATH="/Users/guilhermelimafacanha/workspace/servers/wildfly-node2"
NODE3_PATH="/Users/guilhermelimafacanha/workspace/servers/wildfly-node3"
DOCKER_COMPOSE_DIR="$(cd "$(dirname "$0")" && pwd)"

case "$1" in
  start)
    rm -f "$NODE1_PATH/standalone/log/server.log"
    rm -f "$NODE2_PATH/standalone/log/server.log"
    rm -f "$NODE3_PATH/standalone/log/server.log"
    cp files/servlet.war "$NODE1_PATH/standalone/deployments/ROOT.war"
    cp files/servlet.war "$NODE2_PATH/standalone/deployments/ROOT.war"
    cp files/servlet.war "$NODE3_PATH/standalone/deployments/ROOT.war"
    WILDFLY_NODE_NAME=node1 "$NODE1_PATH/bin/standalone.sh" -c standalone-ha.xml -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.tx.node.id=1 -Djboss.node.name=node1 -Djboss.route=node1 -Djboss.bind.address.private=127.0.0.1 &
    WILDFLY_NODE_NAME=node2 "$NODE2_PATH/bin/standalone.sh" -c standalone-ha.xml -Djboss.socket.binding.port-offset=100 -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.tx.node.id=2 -Djboss.node.name=node2 -Djboss.route=node2 -Djboss.bind.address.private=127.0.0.1 &
    WILDFLY_NODE_NAME=node3 "$NODE3_PATH/bin/standalone.sh" -c standalone-ha.xml -Djboss.socket.binding.port-offset=200 -b 0.0.0.0 -bmanagement 0.0.0.0 -Djboss.tx.node.id=3 -Djboss.node.name=node3 -Djboss.route=node3 -Djboss.bind.address.private=127.0.0.1 &
    (cd "$DOCKER_COMPOSE_DIR" && docker compose up -d)
    ;;
  stop)
    pkill -f "$NODE1_PATH"
    pkill -f "$NODE2_PATH"
    pkill -f "$NODE3_PATH"
    (cd "$DOCKER_COMPOSE_DIR" && docker compose down)
    ;;
  *)
    echo "Usage: $0 {start|stop}"
    exit 1
    ;;
esac