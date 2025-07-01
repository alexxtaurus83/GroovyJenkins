Of course. Here is that text converted to Markdown.

# Groovy Scripts for Jenkins Pipelines
This repo contains a collection of `Groovy` scripts that are used in `Jenkins` pipelines at my home lab. The scripts are grouped into different files based on their functionality.

---
## 📁 `jenkins_utility_functions`
* **`getDurationSeconds(start, end)`**: Calculates the duration in seconds between two `Date` objects.
* **`getRandomString(len)`**: Generates a random alphanumeric string of a specified length.
* **`base64Decode(string)`**: Decodes a Base64 encoded string.
* **`base64Encode(string)`**: Encodes a string to Base64.
* **`printStackTrace(e)`**: Prints the full stack trace of an exception.

---
## 📁 `jenkins_jira_utils`
* **`getJirasByObjectName(...)`**: Retrieves and counts Jira issues associated with a given object (e.g., sprint, fixVersion).
* **`getJira(...)`**: Retrieves detailed information for a single Jira issue.
* **`getSprintOrVersion(...)`**: Searches for and retrieves details about a Jira sprint or version.
* **`doPaginatedJSONRequest(...)`**: Performs paginated HTTP GET requests to JSON APIs.

---
## 📁 `imageHelper`
* **`checkImageExist(...)`**: Checks if a container image exists in a registry and returns its status and size.

---
## 📁 `runGitleaksDetect`
* **`call(buildId, debug)`**: Executes a `Gitleaks` scan on the current codebase to detect secrets.

---
## 📁 `fileHelper`
* **`uploadFile(...)`**: Uploads a local file to a specified URL using an HTTP PUT request.
* **`downloadFile(...)`**: Downloads a file from a given URL using an HTTP GET request.
* **`remoteFileExists(...)`**: Checks if a file exists at a remote URL using an HTTP HEAD request.

---
## 📁 `hashicorp`
* **`_callApi(...)`**: A private helper function for performing generic HTTP requests to API endpoints.
* **`createVaultSecret(...)`**: Creates or updates a secret in HashiCorp Vault.
* **`readVaultSecret(...)`**: Reads a secret value from HashiCorp Vault.
* **`deleteVaultSecrets(...)`**: Deletes specified secrets from HashiCorp Vault.

---
## 📁 `Jenkinsfile-pg-backup`
* A simple example of a `Jenkinsfile` to run scheduled backups for a Postgres DB.