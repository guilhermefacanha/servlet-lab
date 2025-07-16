FROM quay.io/wildfly/wildfly:36.0.1.Final-jdk21

ENV DB_HOST=host.docker.internal

COPY files/postgresql $JBOSS_HOME/modules/org/postgresql
COPY files/standalone.xml $JBOSS_HOME/standalone/configuration/standalone.xml
COPY files/servlet.war $JBOSS_HOME/standalone/deployments/app.war

RUN --mount=type=secret,id=ADMIN_USER,env=ADMIN_USER,required=true             \
    --mount=type=secret,id=ADMIN_PASSWORD,env=ADMIN_PASSWORD,required=true     \
    $JBOSS_HOME/bin/add-user.sh -u ${ADMIN_USER} -p ${ADMIN_PASSWORD} --silent


CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0", "-bmanagement", "0.0.0.0"]