package io.jenkins.mawinter69.pipeline_utils

import hudson.slaves.OfflineCause
import hudson.model.User
import hudson.model.Node
import hudson.model.Computer
import hudson.util.StreamTaskListener
import java.io.ByteArrayOutputStream
import java.util.concurrent.*
import jenkins.model.Jenkins

class Exec implements Callable<ExecResult>
{
    Computer computer
    def node
    def user
    def reason
    def offlineNodes
    def command
    boolean alreadyOffline = false
    String output = ""
    int exitCode = 0

    @NonCPS
    private void waitForBuildsToFinish()
    {
        while (computer.countBusy() > 0)
        {
            sleep(10000)
        }
    }

    @NonCPS
    private takeOffline()
    {
        output += "Taking offline: " + Utils.nodeLink(node) + "\n"
        if (computer.isOffline())
        {
            output += "  Agent state is temporarily offline or not conencted, nothing to do.\n"
            output += "  Offline reason : " + computer.getOfflineCauseReason() + "\n"
            alreadyOffline = true
        }
        else
        {
            def offline_cause = new OfflineCause.UserCause(User.getById(user, false), reason)
            computer.setTemporarilyOffline(true, offline_cause)
        }
    }


    @NonCPS
    private executeCommand()
    {
        ByteArrayOutputStream outputBuffer = null
        ByteArrayOutputStream cmdBuffer = null
        StreamTaskListener outputListener = null
        StreamTaskListener cmdListener = null
        def script = null

        if (computer.getChannel() == null)
        {
            output += "[Warning] Agent not connected: " + Utils.nodeLink(node) + "\n"
            offlineNodes << node
            return
        }
        try 
        {
          outputBuffer = new ByteArrayOutputStream()
          cmdBuffer = new ByteArrayOutputStream()
          outputListener = new StreamTaskListener(outputBuffer)
          cmdListener = new StreamTaskListener(cmdBuffer)

          def launcher = node.createLauncher(cmdListener)

          def env = [:]

          def dir = node.getRootPath()
          script = dir.createTextTempFile("_adm_exec_offline", ".sh", command, true);
          
          def procStarter = new hudson.Launcher.ProcStarter()
              .cmds(["/bin/sh", "-xe", script.getRemote()])
              .envs(env)
              .quiet(true)
              .pwd(dir)
              .stdout(outputListener)

          def proc = launcher.launch(procStarter)
          proc.getStdout()
          exitCode = proc.join()

          output+=outputBuffer.toString()
              
        }
        finally {
          if (script!=null) try { script.delete() } catch (IOException e) {}
          if (outputListener != null) outputListener.closeQuietly()
          if (outputBuffer != null) try { outputBuffer.close() } catch (Exception ex) {}
        }
    }
    
    @NonCPS
    private void takeOnline()
    {
        def count = 0
        if (alreadyOffline)
        {
            output += "[Info] Agent was already offline before, so it will not be taken online\n"
            return
        }
        computer.setTemporarilyOffline(false,null)
        output += "[Info] Taking agent online.\n"
        // Check if agent is connected to Jenkins

        while (!computer.isOnline() && count < 12)
        {
            computer.connect(true)
            output += "[Info] Agent is online but not connected to Jenkins. Sleeping for 10 seconds...\n"
            sleep(10000)
            count++
        }
        if(computer.isOnline())
        {
            output += "[Info] Agent is successfully connected to Jenkins.\n"
        }
        else
        {
            output += "[Info] Agent cannot get connected back to Jenkins.\n"
            exitCode = 1
        }
    }
    
    @NonCPS
    public ExecResult call()
    {
        try {
            computer = node.toComputer()
            takeOffline()
            waitForBuildsToFinish()
            executeCommand()
            takeOnline()
        }
        catch (Exception e)
        {   
            def stackTrace = new StringWriter()
            e.printStackTrace(new PrintWriter(stackTrace))
            output += stackTrace
            exitCode = 1
        }

        return new ExecResult(node, output, exitCode)
    }
}


@NonCPS
def run(String label, String offline_comment, String user, String script) {
 
  echo("""
Parameters:
  Label  : $label
  Reason : $offline_comment
  Script : $script
""")

  def jenkins = Jenkins.get()


  zeroExitNodes = []
  nonZeroExitNodes = []
  errorNodes = []
  cancelledNodes = []
  offlineNodes = []
 
  def nodes = jenkins.getLabel(label).getNodes()
  
  echo ("""
[Info] Processing the following Agents: 
----------------------------------------
${nodes.collect({Utils.nodeLink(it)}).join(", ")}
----------------------------------------
""")

  /** colletc server list **/
  def busy_computers=new Vector()
  def stopped_computers=new Vector()

  ExecutorService executor

  try
  {
      executor = Executors.newFixedThreadPool(20)
      CompletionService<ExecResult> completionService = new ExecutorCompletionService<ExecResult>(executor);
      def futures = new IdentityHashMap()

      for (node in nodes)
      {

          try
          {
              def future = completionService.submit(new Exec(node: node, user: user, command: script, offlineNodes: offlineNodes, reason: offline_comment))
              futures[future] = node
          }
          catch (Exception e)
          {
              echo("problem occured: " + e.getMessage())
          }
      }

      echo("script executions scheduled on ${nodes.size()} agent(s)")

      // process results
      if (futures)
      {
          lastIntermediateStatusAtIndex = -1
          for (int i = 0; i < nodes.size(); i++)
          {
              def future
              while (!future)
              {
                  future = completionService.poll(15, TimeUnit.SECONDS)
                  if (!future && i != lastIntermediateStatusAtIndex)
                  {
                      lastIntermediateStatusAtIndex = i

                      def unfinished = futures.values().sort(false) {a, b -> a.nodeName <=> b.nodeName}
                      echo("still running on ${unfinished.size} agent(s) (${unfinished.collect({Utils.nodeLink(it)}).join(", ")})")
                  }
              }

              node = futures[future]
              futures.remove(future)  // keep only unfinished futures

              StringBuilder message = new StringBuilder()
              message << "*** agent " << Utils.nodeLink(node) << " ***\n"

              try
              {
                  ExecResult result = future.get()

                  (result.exitCode ? nonZeroExitNodes : zeroExitNodes) << node

                  message << "exit code: " << result.exitCode << "\n"

                  message << "output:\n"

                  message << result.output << "\n"
              }
              catch (CancellationException ex)
              {
                  cancelledNodes << node
                  message << "cancelled: " << (ex.message ?: "cause unknown") << "\n"
              }
              catch (ExecutionException ex)
              {
                  errorNodes << node
                  message << "error: " << ex.cause << "\n"
                  ex.printStackTrace()
              }

              echo("Result: " + message.toString())
          }
      }
  }
  finally {
    executor?.shutdownNow()
  }

  echo("""
Summary:
  matching agents : ${nodes.size()} (${nodes.collect({Utils.nodeLink(it)}).join(", ")})
  offline agents  : ${offlineNodes.size} (${offlineNodes.collect({Utils.nodeLink(it)}).join(", ")})
  zero exits      : ${zeroExitNodes.size} (${zeroExitNodes.collect({Utils.nodeLink(it)}).join(", ")})
  non-zero exits  : ${nonZeroExitNodes.size} (${nonZeroExitNodes.collect({Utils.nodeLink(it)}).join(", ")})
  errors          : ${errorNodes.size} (${errorNodes.collect({Utils.nodeLink(it)}).join(", ")})
  cancellations   : ${cancelledNodes.size} (${cancelledNodes.collect({Utils.nodeLink(it)}).join(", ")})
  """)

  if (errorNodes || cancelledNodes || nonZeroExitNodes || offlineNodes)
  {
      error()
  }
}