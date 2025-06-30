// simplified_securitas_vault_api.groovy

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Performs a basic HTTP request to an API endpoint.
 * This function is an internal helper for create/read/delete operations.
 *
 * @param method String: HTTP method (GET, POST, DELETE).
 * @param url String: The target URL.
 * @param authToken String: The authentication token (e.g., X-Vault-Token).
 * @param requestBody String: JSON request body for POST/PUT. Null for GET/DELETE.
 * @param contentType String: Content-Type header (e.g., 'application/json').
 * @param expectedStatus int: Expected successful HTTP status code.
 * @param debug boolean: Enable verbose logging.
 * @return Map: Parsed JSON response if successful, empty map otherwise.
 */
private Map _callApi(String method, String url, String authToken, String requestBody, String contentType, int expectedStatus, boolean debug) {
    def response
    try {
        def headers = [[name: 'X-Vault-Token', value: authToken]]
        if (contentType) {
            headers.add([name: 'Content-Type', value: contentType])
        }
        
        response = httpRequest(
            httpMode: method,
            url: url,
            requestBody: requestBody,
            contentType: contentType,
            customHeaders: headers,
            ignoreSslErrors: true,
            quiet: !debug,
            consoleLogResponseBody: debug,
            validResponseCodes: "${expectedStatus}:600" // Allow expected success code, but also errors for inspection
        )

        if (response.status == expectedStatus) {
            echo "${method} request to ${url} successful (Status: ${response.status})."
            if (response.content) {
                return new JsonSlurper().parseText(response.content) as Map
            } else {
                return [:] // No content for 204 or other empty responses
            }
        } else {
            echo "${method} request to ${url} failed. Expected ${expectedStatus}, got ${response.status}."
            return [:]
        }
    } catch (Exception e) {
        echo "Exception during ${method} request to ${url}: ${e.toString()}"
        return [:]
    }
}

/**
 * Creates or updates a secret in Vault.
 *
 * @param vaultUrlBase String: Base Vault URL (e.g., "https://hashicorpvault.net").
 * @param vaultToken String: The X-Vault-Token for authentication.
 * @param engineType String: Vault secrets engine type (e.g., 'app', 'kv').
 * @param pathPrefix String: The path prefix for the secret (e.g., 'icto/env').
 * @param secretName String: The name of the secret.
 * @param secretValueBase64 String: The base64-encoded secret value.
 * @param debug boolean: Enable verbose logging.
 * @return boolean: True if the secret was created/updated successfully, false otherwise.
 * @dependency utils.base64Decode - required for decoding secretValueBase64.
 */
Boolean createVaultSecret(String vaultUrlBase, String vaultToken, String engineType, String pathPrefix, String secretName, String secretValueBase64, boolean debug) {
    echo "Attempting to create/update secret '${secretName}' in Vault."
    String secretJsonBody = JsonOutput.toJson([data: ["": utils.base64Decode(secretValueBase64)]])
    String url = "${vaultUrlBase}/v1/${engineType}/data/${pathPrefix}/${secretName}"
    Map response = _callApi('POST', url, vaultToken, secretJsonBody, 'application/json', 200, debug)
    return !response.isEmpty()
}

/**
 * Reads a secret value from Vault.
 *
 * @param vaultUrlBase String: Base Vault URL.
 * @param vaultToken String: The X-Vault-Token for authentication.
 * @param engineType String: Vault secrets engine type.
 * @param pathPrefix String: The path prefix for the secret.
 * @param secretName String: The name of the secret to read.
 * @param debug boolean: Enable verbose logging.
 * @return String: The value of the secret, or an empty string if not found or an error occurs.
 */
String readVaultSecret(String vaultUrlBase, String vaultToken, String engineType, String pathPrefix, String secretName, boolean debug) {
    echo "Attempting to read secret: '${secretName}' from Vault."
    String url = "${vaultUrlBase}/v1/${engineType}/data/${pathPrefix}/${secretName}"
    Map response = _callApi('GET', url, vaultToken, null, null, 200, debug)
    if (!response.isEmpty() && response.data?.data) {
        // Assuming the secret value is directly under 'data.data' and there's only one key/value pair
        def secretValue = ""
        response.data.data.each { entry -> secretValue = entry.value }
        echo "Secret obtained from Vault for '${secretName}'."
        return secretValue.toString()
    } else {
        echo "Failed to retrieve secret '${secretName}' or data format unexpected."
        return ""
    }
}

/**
 * Deletes secrets from Vault.
 *
 * @param vaultUrlBase String: Base Vault URL.
 * @param vaultToken String: The X-Vault-Token for authentication.
 * @param engineType String: Vault secrets engine type.
 * @param pathPrefix String: The path prefix for the secrets.
 * @param secretNames List<String>: List of secret names to delete.
 * @param debug boolean: Enable verbose logging.
 * @return boolean: True if all secrets were successfully deleted, false otherwise.
 */
Boolean deleteVaultSecrets(String vaultUrlBase, String vaultToken, String engineType, String pathPrefix, List secretNames, boolean debug) {
    echo "Attempting to delete secrets: '${secretNames}' from Vault."
    boolean allSucceeded = true
    secretNames.each { secretName ->
        String url = "${vaultUrlBase}/v1/${engineType}/metadata/${pathPrefix}/${secretName}" // Use /metadata for delete
        Map response = _callApi('DELETE', url, vaultToken, null, null, 204, debug) // 204 No Content for successful delete
        if (response.isEmpty()) { // _callApi returns empty map on failure
            echo "Failed to delete secret '${secretName}'."
            allSucceeded = false
        } else {
            echo "Secret '${secretName}' deleted successfully."
        }
    }
    return allSucceeded
}

