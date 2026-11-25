///usr/bin/env jbang "$0" "$@" ; exit $?
// Runs Ladder straight from source with JBang: `jbang ladder.java`, or from anywhere with
// `jbang ladder@holly-cummins/ladder-tamboui-java`.
//JAVA 25+
//DEPS dev.tamboui:tamboui-tui:0.7.0
//RUNTIME_OPTIONS --enable-native-access=ALL-UNNAMED
//DEPS dev.tamboui:tamboui-panama-backend:0.7.0
//SOURCES src/main/java/dev/holly/ladder/*.java
//FILES levels/=src/main/resources/levels/*
//MAIN dev.holly.ladder.Main
