package io.jenkins.mawinter69.pipeline_utils

import hudson.model.Computer

class ExecResult
{
    final Computer computer
    final String output
    final int exitCode

    ExecResult(Computer computer, String output, int exitCode)
    {
        this.computer = computer
        this.exitCode = exitCode
        this.output = output
    }
}
