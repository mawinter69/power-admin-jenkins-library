def call(String label, String script, boolean failOnNonZeroExitCode = false, String password = null, boolean offline = false,  boolean failOnError = true) {
  def runner = new io.jenkins.mawinter69.pipeline_utils.ExecScript()
  runner.run(label, script, failOnNonZeroExitCode, password, offline, failOnError)
}

