#!/bin/bash

_operation="some"
_container="wildfly"
_image="wildfly:1.0"
_build_image="wildfly:1.0"
_volume=""
_envVars=""

#port -p host:container_port
_port=" -p 8085:8080 -p 9995:9990"

echo 'params: '$#
echo "$@"
if (( $# <= 0 )); then
    echo 'you must provide one operation: [log, status, create, start, stop, remove, exec]: type _operation'
    read _operation
else
    _operation=$1
fi

echo ' ==== '
echo 'selected _operation: '$_operation
echo ' ==== '

case "$_operation" in
    'create')
    	echo "=>creating $_container container: "
  		docker run -it $_port $_volume $_envVars --name $_container -d $_image

    ;;
    'remove')
        docker rm -f $_container
    ;;
    'build')
        echo "=>building $_build_image image: "
        export ADMIN_USER=admin
        export ADMIN_PASSWORD=admin
        docker build --secret id=ADMIN_USER --secret id=ADMIN_PASSWORD -t $_build_image .
    ;;
    'clean')
        docker rm -f $_container
        docker rmi -f $_image
        docker image prune -f
    ;;
    'log')
	    docker logs $_container
    ;;
    'status')
        docker ps | grep ldap
    ;;
    'start')
	    docker start $_container
    ;;
    'stop')
	    docker stop $_container
    ;;
    'exec')
        docker exec -it $_container bash
    ;;
    *)
        echo _operation $_operation not recognized
    ;;
esac


echo finished!
