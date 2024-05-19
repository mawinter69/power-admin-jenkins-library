def call(String label, String reason, String script) {
  def runner = new io.jenkins.mawinter69.pipeline_utils.ExecAgentScriptOffline()
  runner.run(label, reason, getUser(), script)
}

@NonCPS
def getUser() {
  def cause = currentBuild.rawBuild.getCause(hudson.model.Cause.UserIdCause.class)
  if(cause == null) {
    user = hudson.model.User.get("SYSTEM", false)
  }
  else {
    user = hudson.model.User.get(cause.getUserId(), false)
  }
  return user;
}