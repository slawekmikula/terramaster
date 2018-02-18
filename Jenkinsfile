pipeline {
  agent any
  environment{
      def files = findFiles(glob: '**/terramaster.jar')[0].getPath()
  }
  stages {
    stage( 'build' ) {
      steps{
        bat 'git config --global credential.helper cache'  
        git credentialsId: 'github', url: 'https://github.com/Portree-Kid/terramaster.git'
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        readProperties file: 'build_info.properties'
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME']])
        {
          bat 'git commit -am "Version ${build.major.number}.${build.minor.number}"'
          bat "echo ${env.GIT_USERNAME}"
          //bat "git tag -a some_tag${env.BUILD_NUMBER} -m 'Jenkins'"
          bat "git  -c core.askpass=true  push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/Portree-Kid/terramaster.git"
          //bat ("git -c core.askpass=true push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/some-tag/some-tag.git some_tag${env.BUILD_NUMBER}")
        }
        archiveArtifacts '*terramaster*.jar'    
      }
    }
    
    stage( 'deploy' ) {
      steps{

        withEnv(["SID=${env.sid}"]) {
//           bat 'git show-ref'
           echo """${files}"""
           bat "C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t ${build.major.number}.${build.minor.number}"
           bat """C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t ${build.major.number}.${build.minor.number} -n ${build.major.number}.${build.minor.number} -f ${files}"""
        }
        archiveArtifacts '*terramaster*.jar'
      }
    }
  }
}
