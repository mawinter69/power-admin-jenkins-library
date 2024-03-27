package io.jenkins.mawinter69.pipeline_utils

import hudson.model.Node

class ExecResult
{
    final Node node
    final String output
    final int exitCode

    ExecResult(Node node, String output, int exitCode)
    {
        this.node = node
        this.exitCode = exitCode
        this.output = output
    }
}
