/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  // Run a JVM per core in tests
  forkCount: '1C',
  useContainerAgent: true,
  configurations: [
    [platform: 'linux', jdk: 17],
    [platform: 'linux', jdk: 21, jenkins: '2.414'],
    [platform: 'windows', jdk: 11],
])
