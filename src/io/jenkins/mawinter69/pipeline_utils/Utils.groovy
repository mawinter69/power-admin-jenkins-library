package io.jenkins.mawinter69.pipeline_utils
import hudson.console.HyperlinkNote

@NonCPS
static String nodeLink(Node node) {
  if (node instanceof Jenkins)
  {
    return HyperlinkNote.encodeTo("/computer/(built-in)","built-in")
  }
  else
  {
    return HyperlinkNote.encodeTo('/' + node.getSearchUrl(), node.getDisplayName() )
  }
}

return this