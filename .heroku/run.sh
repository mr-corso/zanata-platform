#!/bin/bash

echo "Build Zanata"

./mvnw clean install -DskipTests -DskipArqTests -DskipFuncTests -Dappserver=wildfly8 -am -pl server/zanata-war
