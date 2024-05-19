package io.jenkins.mawinter69.pipeline_utils

import hudson.model.Node
import hudson.model.Computer
import hudson.util.StreamTaskListener
import java.io.ByteArrayOutputStream
import jenkins.model.Jenkins
import java.util.concurrent.Callable

/**
 * Executes a script on the given node. Linux only.
 * Optionally the script can be run as root user by providing a password that will be used for sudo
 */
class ExecScriptOnNode implements Callable<ExecResult>
{
  final Node node
  final boolean verbose
  final String password
  final String script

  /**
   * Parameters:
   *  node: the node where to execute the script
   *  script: the script to execute
   *  verbose: be more verbose
   *  password: the password for sudo
   */
  public ExecScriptOnNode(Node node, String script, boolean verbose, String password)
  {
    this.node = node
    this.verbose = verbose
    this.password = password
    this.script = script
  }

  @NonCPS 
  public ExecResult call() {
    return run()
  }

  /**
   * executes the script.
   *
   * returns an "ExecResult" with node, exitcode and output
   */
  @NonCPS
  public ExecResult run()
  {
    ByteArrayOutputStream outputBuffer = null
    ByteArrayOutputStream cmdBuffer = null
    StreamTaskListener outputListener = null
    StreamTaskListener cmdListener = null
    def output = ""
    def wrapperScriptFile;
    def scriptFile

    try 
    {
      outputBuffer = new ByteArrayOutputStream()
      cmdBuffer = new ByteArrayOutputStream()
      outputListener = new StreamTaskListener(outputBuffer)
      cmdListener = new StreamTaskListener(cmdBuffer)

      def launcher = node.createLauncher(cmdListener)

      def env = [:]
      def execScript
      def dir = node.getRootPath()

      scriptFile = dir.createTextTempFile("_adm_exec", ".sh", script, true);
      execScript = scriptFile.getRemote();

      if (password != null){
          def wrapperScript = """
          echo "\$SUDO_PW"|sudo -S /bin/sh -xe "$execScript"
          """
          wrapperScriptFile = dir.createTextTempFile("_adm_exec_wrapper", ".sh", wrapperScript, true);
          execScript = wrapperScriptFile.getRemote()
          env["SUDO_PW"] = password

      }
      
      def procStarter = new hudson.Launcher.ProcStarter()
          .cmds(["/bin/sh", "-xe", execScript])
          .envs(env)
          .quiet(true)
          .stdout(outputListener)

      def proc = launcher.launch(procStarter)
      proc.getStdout()
      int exit_code = proc.join()

      if (verbose){
          output+=cmdBuffer.toString()
      }
      
      output+=outputBuffer.toString()
          
      return new ExecResult(node, exit_code, output)
    }
    finally {
      if (wrapperScriptFile!=null) try { wrapperScriptFile.delete() } catch (IOException e) {}
      if (scriptFile!=null) try { scriptFile.delete() } catch (IOException e) {}
      if (outputListener != null) outputListener.closeQuietly()
      if (outputBuffer != null) try { outputBuffer.close() } catch (Exception ex) {}
    }
  }
}

