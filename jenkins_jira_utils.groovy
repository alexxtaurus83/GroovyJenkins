// Retrieves a summary of Jira issues associated with a given object name (e.g., sprint, fixVersion).

//
// Parameters:
//   odysseyId (String): The Jira project ID (e.g., 'MYPROJECT').
//   jenkinsSecretToAccessJira (String): Jenkins credentialsId for Jira API access.
//   searchObject (String): The Jira field to search by (e.g., 'sprint', 'fixVersion').
//   searchObjectName (String): The name of the object to search for (e.g., 'Sprint 1', 'v1.0').
//   debug (Boolean): If true, enables verbose logging.
//
// Returns:
//   Map: A map containing 'totalIssues', 'bugs', and 'closed' issue counts.
//
// Dependencies:
//   - Jenkins Pipeline 'echo', 'withCredentials', 'httpRequest', 'readJSON' steps.
//   - Assumes 'utils.base64Encode' and 'utils.printStackTrace' are available.
Map getJirasByObjectName(String odysseyId, String jenkinsSecretToAccessJira, String searchObject, String searchObjectName, Boolean debug = false) {
    if (debug) {
        echo """
searchObject: ${searchObject}
searchObjectName: ${searchObjectName}
"""
    }
    def returnMap = ["totalIssues": 0, "bugs": 0, "closed": 0 ]
    try {
        // Defined Jira statuses considered "done"
        def doneStatuses= ["Done","Approved", "Cancelled", "Rejected", "Published", "Accepted", "Cannot reproduce", "Closed"]

        int startAt = 0
        int maxResults = 500
        int total = 1 // Initialize total > startAt to ensure the loop runs at least once

        def bodyTemplate = """
{
    \"jql\": "project = ${odysseyId} AND ${searchObject} = \\"${searchObjectName}\\" AND issuetype != \\"Sub-Task\\"",
    \"startAt\": \${startAt},
    \"maxResults\": \${maxResults},
    \"fields\": [
        \"summary\",
        \"issuetype\",
        \"status\",
        \"resolution\"
    ]
}
"""
        withCredentials([usernamePassword(credentialsId: jenkinsSecretToAccessJira, usernameVariable: 'JIRA_CREDS_USERNAME', passwordVariable: 'JIRA_CREDS_PASSWORD')]) {
            // Encode Jira credentials for Basic Authentication. Requires 'utils.base64Encode'.
            def creds = utils.base64Encode("${JIRA_CREDS_USERNAME}:${JIRA_CREDS_PASSWORD}")

            while (startAt <= total) {
                // Generate request body for current pagination
                def body = evaluate(new org.jenkinsci.plugins.scriptsecurity.sandbox.groovy.SecureGroovyScript(bodyTemplate, true, null).script)

                def response = httpRequest quiet : !debug, consoleLogResponseBody: debug, ignoreSslErrors: true,
                                customHeaders: [[name: 'Authorization', value: "Basic ${creds}"]],
                                httpMode: 'POST', acceptType: 'APPLICATION_JSON', contentType: 'APPLICATION_JSON',
                                requestBody: body, url: "https://jira.com/rest/api/2/search" // Hardcoded Jira URL

                if (response.status != 200 ) {
                    echo "Unable to find any JIRAs associated with ${searchObject}:${searchObjectName} for ${odysseyId}. HTTP Status: ${response.status}"
                    total = 0 // Stop loop on HTTP error
                } else {
                    def resp = readJSON text: response.content, returnPojo: true
                    total = resp.total // Update total from Jira response for pagination

                    if (total == 0) {
                        echo "No JIRAs found associated with ${searchObject}:${searchObjectName} for ${odysseyId}."
                    } else {
                        echo "${resp.total} JIRAs found associated with ${searchObject}:${searchObjectName} for ${odysseyId}."
                        returnMap.totalIssues = total
                        resp.issues.each { item ->
                            if (debug) { echo "Processing Jira: ${item.key} - Issue Type: ${item.fields.issuetype.name}, Status: ${item.fields.status.name}." }
                            if (item.fields.issuetype.name.toLowerCase().equals("bug")) {
                                returnMap.bugs ++
                            }
                            if (item.fields.resolution) { // Resolution field indicates the issue is closed/resolved
                                if (debug) { echo "${item.key} is resolved."}
                                returnMap.closed ++
                            } else {
                                if (debug) { echo "${item.key} is not resolved."}
                            }
                        }
                    }
                }
                startAt += maxResults // Increment start for next page
            }
        }
    }
    catch (Exception e) {
        echo 'Exception occurred in getJirasByObjectName: ' + e.toString()
        if (debug) {
            utils.printStackTrace(e) // Requires 'utils.printStackTrace'
        }
    }
    if (debug) {
        echo "Final result for Jiras by Object Name: ${returnMap}"
    }
    return returnMap
}

// Retrieves details for a single Jira issue.

//
// Parameters:
//   jenkinsSecretToAccessJira (String): Jenkins credentialsId for Jira API access.
//   jira (String): The Jira issue key (e.g., 'MYPROJECT-123').
//   debug (Boolean): If true, enables verbose logging.
//
// Returns:
//   Map: A map containing 'id', 'fixVersions', 'assignee', 'status', 'project', and 'summary' of the Jira.
//        Returns default empty values if the Jira is not found or an error occurs.
//
// Dependencies:
//   - Jenkins Pipeline 'echo', 'withCredentials', 'httpRequest', 'readJSON' steps.
//   - Assumes 'utils.base64Encode' and 'utils.printStackTrace' are available.
Map getJira(String jenkinsSecretToAccessJira, String jira, Boolean debug = false) {
    def returnMap = ["id": 0, "fixVersions": "", "assignee":"", "status":"", "project":"", "summary":"" ]
    try {
        withCredentials([usernamePassword(credentialsId: jenkinsSecretToAccessJira, usernameVariable: 'JIRA_CREDS_USERNAME', passwordVariable: 'JIRA_CREDS_PASSWORD')]) {
            def creds = utils.base64Encode("${JIRA_CREDS_USERNAME}:${JIRA_CREDS_PASSWORD}") // Requires 'utils.base64Encode'
            def response = httpRequest quiet : !debug, consoleLogResponseBody: debug, ignoreSslErrors: true,
                            customHeaders: [[name: 'Authorization', value: "Basic ${creds}"]],
                            url: "https://jira.com/rest/api/2/issue/${jira}" // Hardcoded Jira URL

            if (response.status != 200 ) {
                echo "Unable to find Jira issue: ${jira}. HTTP Status: ${response.status}"
            } else {
                def resp = readJSON text: response.content, returnPojo: true
                echo "Jira issue '${jira}' found."
                returnMap.id = Integer.parseInt(resp.id)
                returnMap.fixVersions = resp.fields.fixVersions
                returnMap.assignee = resp.fields.assignee.displayName
                returnMap.status = resp.fields.status.statusCategory.name
                returnMap.project = resp.fields.project.key
                returnMap.summary = resp.fields.summary
            }
        }
    }
    catch (Exception e) {
        echo 'Exception occurred in getJira: ' + e.toString()
        if (debug) {
            utils.printStackTrace(e) // Requires 'utils.printStackTrace'
        }
    }
    if (debug) {
        echo "Result for Jira '${jira}': ${returnMap}"
    }
    return returnMap
}

// Searches for a Jira Sprint or Version associated with a given object name within a project.
// It iterates through project versions and then through sprints on boards to find a match.
// This function is for querying Jira Agile and Core APIs within a Jenkins Pipeline.
//
// Parameters:
//   odysseyId (String): The Jira project ID.
//   objectName (String): The name of the sprint or version to search for (case-insensitive, regex-compatible).
//   jenkinsSecretToAccessJira (String): Jenkins credentialsId for Jira API access.
//   debug (boolean): If true, enables verbose logging.
//
// Returns:
//   Map: A map containing details about the found sprint/version (type, name, isClosed)
//        along with totalIssues, bugs, and closed counts obtained by calling getJirasByObjectName.
//        Returns an empty map if no matching object is found.
//
// Dependencies:
//   - Jenkins Pipeline 'echo', 'withCredentials', 'readJSON' steps.
//   - Assumes 'utils.base64Encode' and 'utils.printStackTrace' are available.
//   - Relies on 'doPaginatedJSONRequest' and 'getJirasByObjectName' (defined in this script).
Map getSprintOrVersion(String odysseyId, String objectName, String jenkinsSecretToAccessJira, boolean debug = false) {
    // Helper closure to check if an item (sprint/version) matches the searchObjectName
    def checkForObjectClosure = {item, searchObject, searchObjectName, messageSuffix ->
        Map type = [:]
        if (debug) {
            echo "Current ${searchObject}: ${item.name}, Looking for case-insensitive regex pattern ${searchObjectName}"
        }
        // Uses regex matching for flexibility (e.g., "sprint.1" could match "Sprint 1")
        if (item.name.toLowerCase() ==~ searchObjectName) {
            if (debug) {
                echo "Found ${searchObject}:'${searchObjectName}' for ${odysseyId}${messageSuffix}"
            }
            type = [type: "${searchObject}", name: item.name, isClosed: item.released ?: (item.state == 'closed')] // 'released' for versions, 'state' for sprints
        }
        return type
    }

    // Helper closure to check sprints on a given Jira board with pagination.
    def checkBoardClosure = {searchObject, searchObjectName, creds, boardId, boardName ->
        Map type = [:]
        // Initial URL for paginated request
        def resp = [isLast: false, nextPage: "https://jira.com/rest/agile/1.0/board/${boardId}/${searchObject}${searchObject.equals('sprint') ? '?state=active' : '?released=false'}"]
        while (type == [:] && !resp.isLast) {
            // Get next page of results. Uses 'doPaginatedJSONRequest'.
            resp = doPaginatedJSONRequest(resp.nextPage, creds, "Unable to get ${searchObject}s for board ${boardId} in ${odysseyId}", debug)
            // Iterate through items on the current page
            for (int i=0; type==[:] && i<resp.values.size(); i++) {
                type = checkForObjectClosure(resp.values[i], searchObject, searchObjectName, " and board: ${boardId}")
            }
        }
        return type
    }

    Map type = [:]
    try {
        // Normalize the objectName for case-insensitive regex matching (replaces spaces/hyphens with dots)
        def searchObjectName = objectName.toLowerCase().replaceAll(/\/|_|-| /, '.')
        withCredentials([usernamePassword(credentialsId: jenkinsSecretToAccessJira, usernameVariable: 'JIRA_CREDS_USERNAME', passwordVariable: 'JIRA_CREDS_PASSWORD')]) {
            def creds = utils.base64Encode("${JIRA_CREDS_USERNAME}:${JIRA_CREDS_PASSWORD}") // Requires 'utils.base64Encode'

            // First, check Jira versions for the project
            def resp = [isLast: false, nextPage: "https://jira.com/rest/api/2/project/${odysseyId}/version"] // Hardcoded Jira URL
            while (type == [:] && !resp.isLast) {
                resp = doPaginatedJSONRequest(resp.nextPage, creds, "Unable to get versions for ${odysseyId}", debug) // Uses 'doPaginatedJSONRequest'
                for (int i=0; type==[:] && i<resp.values.size(); i++) {
                    type = checkForObjectClosure(resp.values[i], 'version', searchObjectName, "")
                }
            }

            // If no version found, check sprints across all boards in the project
            if (type == [:]) { // Check if type is still empty, not null.
                resp = [isLast: false, nextPage: "https://jira.com/rest/agile/1.0/board?projectKeyOrId=${odysseyId}"] // Hardcoded Jira URL
                while (type == [:] && !resp.isLast) {
                    resp = doPaginatedJSONRequest(resp.nextPage, creds, "Unable to get boards for ${odysseyId}", debug) // Uses 'doPaginatedJSONRequest'
                    for (int i = 0; i < resp.values.size() && type == [:]; i++) {
                        def item = resp.values[i]
                        // Only check scrum boards, not kanban
                        if (!item.type.equals("kanban")) {
                            if (debug) {
                                echo "Checking board ${item.name} (ID: ${item.id})"
                            }
                            type = checkBoardClosure('sprint', searchObjectName, creds, item.id, item.name) // Uses 'checkBoardClosure'
                        }
                    }
                }
            }
            if (type != [:]) {
                if (debug) {
                    echo "Found matching object: ${type}"
                }
                // Once found, get associated Jira issues for that sprint/version
                type = type + getJirasByObjectName(odysseyId, jenkinsSecretToAccessJira, (type.type == 'version' ? 'fixVersion' : type.type), type.name, debug) // Uses 'getJirasByObjectName'
            }
        }
    } catch (Exception e) {
        echo 'Exception occurred in getSprintOrVersion: ' + e.toString()
        if (debug) {
            utils.printStackTrace(e) // Requires 'utils.printStackTrace'
        }
    }
    return type // Return empty map if nothing found, or populated map with details.
}

// A generic function to perform paginated HTTP GET requests to JSON APIs.
// It retrieves JSON data page by page until all data is collected or no more pages are available.
// This is for interacting with any REST API that supports pagination.
//
// Parameters:
//   url (String): The initial URL for the API endpoint.
//   creds (String): Base64 encoded credentials (e.g., for Basic Auth).
//   errMessage (String): Error message to log if the request fails.
//   debug (boolean): If true, enables verbose logging of HTTP responses.
//
// Returns:
//   Map: A map containing the combined JSON data from all pages. Also includes 'isLast' (boolean)
//        and 'nextPage' (String) to facilitate external pagination control, though this implementation
//        handles internal pagination implicitly.
//
// Dependencies:
//   - Jenkins Pipeline 'httpRequest', 'readJSON', 'echo' steps.
Map doPaginatedJSONRequest(String url, String creds, String errMessage, boolean debug) {
    def response = httpRequest quiet : !debug, validResponseCodes: '200:600', ignoreSslErrors: true,
                    customHeaders: [[name: 'Authorization', value: "Basic ${creds}"]],
                    consoleLogResponseBody: debug, url: url // Jenkins 'httpRequest' step
    def jsonData = [:]
    if (response.status != 200 ) {
        echo "${errMessage}. HTTP Status: ${response.status}"
    } else {
        jsonData = readJSON text: response.content, returnPojo: true // Jenkins 'readJSON' step
    }
    return jsonData
}