# Execute script offline

### Description

Executes the same script on a number of agents in parallel. Before running the script the job will take the agent temporarily offline, wait for all builds to finish, execute the script and then take it back online. The execution happens independent of the nodes executors.

If an agent was temporarily offline before it will not be taken online afterwards. Also the offline cause reason will not be changed.

At the start a summary of parameters and matching agents is printed.

Once the script has finished running on an agent the output of that script execution is printed to the console.

At the end a summary is printed with information of matched agents, offline agents (script was not executed), if the script run successful (exit code 0) 
or not.

The step fails when it was not run successfully on all matching agents.

### Limitations
- This step only supports Unix agents
- No Jenkins specific env variables are currently available during script execution, especially there is no WORKSPACE for apparent reasons

### Details
The given script is saved in the agents root directory as a temporary file and then executed with `/bin/sh -xe <file>` with the agent root directory being the current directory.

### Syntax 
```
executeScriptOffline(label, reason, script)
```


### Parameters
| Name | Description |
| ---- | ----------- |
| `label` | A Jenkins label expression, e.g. build && linuxx86_64 |
| `reason` | The message that will be set as the offline cause |
| `script` | The script to execute, can be multiline. |
