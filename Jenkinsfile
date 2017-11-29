

withAnt(installation: 'myinstall') {
    dir("scoring") {
    if (isUnix()) {
      sh "ant mytarget"
    } else {
      bat "ant mytarget"
    }
}