node {
    stage 'Build' {
      withAnt(installation: 'apache-ant-1.10.1') {
        dir("scoring") {
          if (isUnix()) {
            sh "ant mytarget"
          } else {
            bat "ant mytarget"
          }
        }
      }
    }  
  }


