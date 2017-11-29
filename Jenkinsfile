pipeline {
  agent any
  stages {
    stage( 'build' ) {
      steps{
        withAnt('installation' : 'apache-ant-1.10.1') {
          bat "ant default"
        }
      }
    }
  }
}
