pipeline {
   agent any
   stages {
     withAnt('installation' : 'apache-ant-1.10.1') {
       bat "ant default"
     }
  }
}
