#!/usr/bin/env groovy

/* `buildPlugin` step provided by: https://github.com/jenkins-infra/pipeline-library */
buildPlugin(configurations: [
  // Despite the plugin builds with java.level 7, it needs to be run with 8+ to overcome
  // https://stackoverflow.com/questions/50824789/why-am-i-getting-received-fatal-alert-protocol-version-or-peer-not-authentic
  [ platform: "linux", jdk: "8", jenkins: null ],
  [ platform: "windows", jdk: "8", jenkins: null ],
  [ platform: "linux", jdk: "11", jenkins: "2.150" ]
])
