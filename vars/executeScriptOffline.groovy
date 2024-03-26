def call(String label, String reason, String script) {
  def user = getUser()
  def runner = new io.jenkins.mawinter69.pipeline_utils.ExecAgentCommandOffline()
  runner.run(label, reason, user.getId(), script)
}

@NonCPS
def getUser() {
  def cause = currentBuild.rawBuild.getCause(hudson.model.Cause.UserIdCause.class)
  if(cause == null) {
    user = hudson.model.User.get("SYSTEM")
  }
  else {
    user = hudson.model.User.get(cause.getUserId())
  }
}