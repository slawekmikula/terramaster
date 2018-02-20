pipeline {
  agent any
  environment{
      def files = findFiles(glob: '**/terramaster.jar')[0].getPath()
  }
  stages {
    stage( 'build' ) {
      steps{
        bat 'git config --global credential.helper cache'
        bat 'git status'  
        git credentialsId: 'github', url: "${env.GIT_URL}", branch: "${env.GIT_BRANCH}"
        bat 'git status'  
        withEnv(["JAVA_HOME=${ tool 'jdk1.8.0_121' }"]) {
          withAnt('installation' : 'apache-ant-1.10.1') {
            bat "ant default"
          }
        }  
        echo "URL : ${env.GIT_URL}#${env.GIT_BRANCH}"
        readProperties file: 'build_info.properties'
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME']])
        {
          bat 'git status'  
          bat "git add build_info.properties"
          bat "git commit -am 'Version ${build.major.number}.${build.minor.number}'"
          bat 'git status'  
          bat "git  -c core.askpass=true  push https://${env.GIT_USERNAME}:${env.GIT_PASSWORD}@github.com/Portree-Kid/terramaster.git#${env.GIT_BRANCH}"
          bat 'git status'  
        }
        archiveArtifacts '*terramaster*.jar'    
      }
    }
    
    stage( 'deploy' ) {
      steps{
        withEnv(["SID=${env.sid}"]) {
           bat "C:\\Users\\keith.paterson\\go\\bin\\github-release release -s %SID% -u Portree-Kid -r terramaster -t ${build.major.number}.${build.minor.number}"
           bat """C:\\Users\\keith.paterson\\go\\bin\\github-release upload -s %SID% -u Portree-Kid -r terramaster -t ${build.major.number}.${build.minor.number} -n terramaster.jar -f ${files}"""
        }
        archiveArtifacts '*terramaster*.jar'
      }
    }
  }
}
