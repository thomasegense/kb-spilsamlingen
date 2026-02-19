#!/usr/bin/env bash

cd /tmp/src

cp -rp -- /tmp/src/target/kb-spilsamlingen-*.war "$TOMCAT_APPS/kb-spilsamlingen.war"
cp -- /tmp/src/conf/ocp/kb-spilsamlingen.xml "$TOMCAT_APPS/kb-spilsamlingen.xml"

export WAR_FILE=$(readlink -f "$TOMCAT_APPS/kb-spilsamlingen.war")
