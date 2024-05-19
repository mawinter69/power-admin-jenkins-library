package io.jenkins.mawinter69.pipeline_utils

import hudson.console.HyperlinkNote
import jenkins.model.Jenkins

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


/**
 * Get the password of a usernamepassword credential for the given credentialsId
 */
@NonCPS
static String getPassword(String credentialsId) {
def creds = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
    com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials.class,
    Jenkins.get()
)
def c = creds.findResult { it.id == credentialsId ? it : null }

if ( c ) {
    def systemCredentialsProvider = Jenkins.get().getExtensionList(
        'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
        ).first()

    def password = systemCredentialsProvider.credentials.findResult { it.id == credentialsId ? it : null }.password
    return password
} else {
    println "could not find credential for ${credentialsId}"
}
return null
}

return this