package io.jenkins.mawinter69.pipeline_utils

import hudson.slaves.OfflineCause
import hudson.model.User
import hudson.model.Node
import hudson.model.Computer
import java.util.concurrent.*
import jenkins.model.Jenkins


@NonCPS
def run(String label, String script, boolean failOnNonZeroExitCode, String password, boolean offline, boolean failOnError) {
 
  echo("""
Parameters:
  Label  : $label
  Script : $script
  Offline: $offline
""")

  def jenkins = Jenkins.get()
  def verbose = false


  zeroExitNodes = []
  nonZeroExitNodes = []
  errorNodes = []
  cancelledNodes = []
  offlineNodes = []
  onlineNodes = []
 
  nodes = []
  if (label == "all") {
    nodes = jenkins.nodes
  } else {
    nodes = jenkins.getLabel(label).getNodes()
  }

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

  try {
    executor = Executors.newFixedThreadPool(150)
    CompletionService<ExecResult> completionService = new ExecutorCompletionService<ExecResult>(executor);
    def futures = new IdentityHashMap()

    // schedule
    echo """
scheduling command executions on ${nodes.size()} node(s) ...
"""
    
    for (node in nodes) {
      def computer = node.toComputer()
      if (computer == null || computer.getChannel() == null) {
        offlineNodes << node
        continue
      }

      if (!computer.isOnline() && !offline ) {
        offlineNodes << node
        continue
      }
      onlineNodes << node

      def future = completionService.submit(new ExecScriptOnNode(node, script, false, password))
      futures[future] = node
    }
    
    echo """
command executions scheduled on ${onlineNodes.size} node(s), ${offlineNodes.size} node(s) offline
    """


    // process results
    lastIntermediateStatusAtIndex = -1
    for (int i = 0; i < onlineNodes.size; i++) {
      def future
      while (!future) {
        future = completionService.poll(15, TimeUnit.SECONDS)

        if (!future && i != lastIntermediateStatusAtIndex) {
          lastIntermediateStatusAtIndex = i

          def unfinished = futures.values().sort(false) {a, b -> a.nodeName <=> b.nodeName}

          echo """
still running on ${unfinished.size} node(s) (${unfinished.collect({Utils.nodeLink(it)}).join(", ")})

          """
        }
      }

      node = futures[future]
      futures.remove(future)  // keep only unfinished futures

      StringBuilder message = new StringBuilder()
      message << "*** node " << Utils.nodeLink(node) << " ***\n"
      if (verbose) {
        message <<"\n"
      }

      try {
      ExecResult result = future.get()

      (result.exitCode ? nonZeroExitNodes : zeroExitNodes) << node

      message << "exit code: " << result.exitCode << "\n"
      if (verbose) {
        message << "output:\n"
      }
      message << result.output << "\n"
      }
      catch (CancellationException ex) {
        cancelledNodes << node
        message << "cancelled: " << (ex.message ?: "cause unknown") << "\n"
      }
      catch (ExecutionException ex) {
        errorNodes << node
        message << "error: " << ex.cause << "\n"
      }

      echo """
${message}

      """
    }
  }
  finally {
    executor?.shutdownNow()
  }



  echo("""
Summary:
  matching agents : ${nodes.size()} (${nodes.collect({Utils.nodeLink(it)}).join(", ")})
  online nodes    : ${onlineNodes.size} (${onlineNodes.collect({Utils.nodeLink(it)}).join(", ")})
  offline agents  : ${offlineNodes.size} (${offlineNodes.collect({Utils.nodeLink(it)}).join(", ")})
  zero exits      : ${zeroExitNodes.size} (${zeroExitNodes.collect({Utils.nodeLink(it)}).join(", ")})
  non-zero exits  : ${nonZeroExitNodes.size} (${nonZeroExitNodes.collect({Utils.nodeLink(it)}).join(", ")})
  errors          : ${errorNodes.size} (${errorNodes.collect({Utils.nodeLink(it)}).join(", ")})
  cancellations   : ${cancelledNodes.size} (${cancelledNodes.collect({Utils.nodeLink(it)}).join(", ")})
  """)

  if (failOnError && errorNodes) {
    error("Execute Script failed with error")
  }
  if (failOnNonZeroExitCode && nonZeroExitNodes) {
    error("Execute Script failed with non zero return code")
  }
}