FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk21

ENV DB_HOST=host.docker.internal
ENV JBOSS_BIND_ADDRESS_PRIVATE="127.0.0.1"

COPY files/postgresql $JBOSS_HOME/modules/org/postgresql
COPY files/standalone-ha.xml $JBOSS_HOME/standalone/configuration/standalone-ha.xml
COPY files/servlet.war $JBOSS_HOME/standalone/deployments/ROOT.war

RUN --mount=type=secret,id=ADMIN_USER,env=ADMIN_USER,required=true             \
    --mount=type=secret,id=ADMIN_PASSWORD,env=ADMIN_PASSWORD,required=true     \
    $JBOSS_HOME/bin/add-user.sh -u ${ADMIN_USER} -p ${ADMIN_PASSWORD} --silent


CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-c", "standalone-ha.xml", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0", "-bprivate", "0.0.0.0", "-bunsecured", "0.0.0.0", "-Djboss.node.name=${WILDFLY_NODE_NAME}", "-Djboss.route=${WILDFLY_NODE_NAME}", "-Djboss.tx.node.id=${JBOSS_TX_NODE_ID}", "-Djboss.bind.address.private=${JBOSS_BIND_ADDRESS_PRIVATE}"]