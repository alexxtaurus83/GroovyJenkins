This repo contains a collection of Groovy scripts that are used in Jenkins pipelines at my home lab. The scripts are grouped into different files based on their functionality.

File: jenkins_utility_functions
getDurationSeconds(start, end): Calculates the duration in seconds between two Date objects.
getRandomString(len): Generates a random alphanumeric string of a specified length.
base64Decode(string): Decodes a Base64 encoded string.
base64Encode(string): Encodes a string to Base64.
printStackTrace(e): Prints the full stack trace of an exception.

File: jenkins_jira_utils
getJirasByObjectName(odysseyId, jenkinsSecretToAccessJira, searchObject, searchObjectName, debug): Retrieves and counts Jira issues associated with a given object (e.g., sprint, fixVersion).
getJira(jenkinsSecretToAccessJira, jira, debug): Retrieves detailed information for a single Jira issue.
getSprintOrVersion(odysseyId, objectName, jenkinsSecretToAccessJira, debug): Searches for and retrieves details about a Jira sprint or version.
doPaginatedJSONRequest(url, creds, errMessage, debug): Performs paginated HTTP GET requests to JSON APIs.

File: imageHelper
checkImageExist(server, repo, imageWithTag, serverSecret, debug): Checks if a container image exists in a registry and returns its status and size.

File: runGitleaksDetect
call(buildId, debug): Executes a Gitleaks scan on the current codebase to detect secrets.

File: fileHelper
uploadFile(fileUrl, fileName, secretName, contentTp, debug): Uploads a local file to a specified URL using an HTTP PUT request.
downloadFile(fileUrl, secretName, outFile, debug): Downloads a file from a given URL using an HTTP GET request.
remoteFileExists(fileUrl, secretName, debug): Checks if a file exists at a remote URL using an HTTP HEAD request.

File: hashicorp
_callApi(method, url, authToken, requestBody, contentType, expectedStatus, debug): A private helper function for performing generic HTTP requests to API endpoints.
createVaultSecret(vaultUrlBase, vaultToken, engineType, pathPrefix, secretName, secretValueBase64, debug): Creates or updates a secret in HashiCorp Vault.
readVaultSecret(vaultUrlBase, vaultToken, engineType, pathPrefix, secretName, debug): Reads a secret value from HashiCorp Vault.
deleteVaultSecrets(vaultUrlBase, vaultToken, engineType, pathPrefix, secretNames, debug): Deletes specified secrets from HashiCorp Vault.

File: Jenkinsfile-pg-backup
Simple example of jenkins file to run scheduled backups for Postgres DB.