# Execute script offline

### Description

Executes the same script on a number of agents in parallel. This execution happens independent of the nodes executors, i.e. also when all executors are busy or when the agent is temporarily offline, the script will be executed.

At the start a summary of parameters and matching agents is printed.

Once the script has finished running on an agent the output of that script execution is printed to the console.

At the end a summary is printed with information of matched agents, offline agents (script was not executed), if the script run successful (exit code 0) or not.

The step fails when it was not run successfully on all matching agents.

### Limitations
- This step only supports Unix agents
- No Jenkins specific env variables are currently available during script execution, especially there is no WORKSPACE for apparent reasons

### Details
The given script is saved in the agents root directory as a temporary file and then executed with `/bin/sh -xe <file>` with the agent root directory being the current directory.

### Syntax 
```
executeScript(label, reason)
executeScript(label, reason, failOnNonZeroExitCode, password, offline, failOnError)9000000
```

### Parameters
| Name | Description | Default | 
| ---- | ----------- | ------- |
| `label` | A Jenkins label expression, e.g. `build && linuxx86_64`, when you pass `all` it will run on all nodes except the `built-in` | |
| `script` | The script to execute, can be multiline. | | 
| `failOnNonZeroExitCode` | Fail when the script returns an exit code > 0 | `false` |
| `offline` | Run also on temporarily offline nodes | false |
| `password` | Password for sudo | `null` | 
| `failOnError` | Fail on Execution Exception usually never happens | `true`| 


### Example
```
withCredentials([string(credentialsId: 'sudo', variable: 'SUDO_PW')]) {
    def script = '''
    echo "cleaning cache"
    rm -rf /data/app-cache
    '''
    executeScript("linux", script, false, SUDO_PW, true)
}
```