// Defines a function to check if a container image exists in a registry and get its size.
// Parameters:
//   server (String): The hostname of the container registry.
//   repo (String): The repository path in the registry.
//   imageWithTag (String): The image name and tag (e.g., 'my-app:latest').
//   serverSecret (String): The secret for accessing the registry. For ACR
//   debug (boolean): If true, enables verbose logging.
//
// Returns:
//   Map: A map with 'status' (boolean, true if image exists) and 'imageSize' (integer, size in MB).
//
// Dependencies:
//   - Jenkins Pipeline 'httpRequest', 'readJSON', 'sh', 'echo', 'error' steps.
//   - Assumes 'utils.getRandomString' (from jenkins_utility_functions.groovy) is available.
//   - Assumes 'fileHelper.downloadFile' is available (external dependency).
//   - Relies on specific registry API manifest structures for size calculation.
def Map checkImageExist(String server, String repo, String imageWithTag, String serverSecret, boolean debug = false) {
    if (debug) {
        echo """
            Running checkImageExist with args:
                server        ${server}
                repo:         ${repo}
                imageWithTag: ${imageWithTag}
                serverSecret: (hidden)
                debug:        ${debug}
        """
    }
    try {
        def result = [ status : false, imageSize : 0]
        def imageStatus = 0
        // Requires 'utils.getRandomString' from jenkins_utility_functions.groovy
        def jsonFileName = "${imageWithTag.tokenize(':')[0]}-${imageWithTag.tokenize(':')[1]}_${utils.getRandomString(6)}.json"
        def imageSize = 0

        // Logic split based on registry type (ACR vs. others)
        if (server.contains("azurecr")) {
            def acrImageUrl = "https://${server}/v2/${repo}/${imageWithTag.tokenize(':')[0]}/manifests/${imageWithTag.tokenize(':')[1]}"
            // serverSecret is treated as a raw base64 encoded token here
            def body = "${serverSecret}".bytes.encodeBase64().toString()
            imageStatus= httpRequest quiet : true, httpMode: 'GET',validResponseCodes: "200,404", ignoreSslErrors: true, consoleLogResponseBody: debug, customHeaders: [[name: 'Authorization', value: "Basic ${body}"]], url: acrImageUrl
        }
        else {
            // Assumes 'fileHelper.downloadFile' is an external function
            imageStatus = fileHelper.downloadFile("https://${server}/${repo}/${imageWithTag.tokenize(':')[0]}/${imageWithTag.tokenize(':')[1]}/manifest.json", serverSecret, jsonFileName, debug)
        }

        if (debug) {
            echo "image status = ${imageStatus}"
        }

        if (imageStatus.toString().equals('200')) {
            echo("Image ${imageWithTag} exists at server ${server}")
            def manifest = readJSON file: jsonFileName // Requires Jenkins 'readJSON' step
            manifest.layers.each { layer ->
                imageSize = imageSize + layer.size
            }
            imageSize = manifest.config.size ? imageSize + manifest.config.size : imageSize
            result.status = true
            result.imageSize = (imageSize / 1024 / 1024).toString().tokenize('.')[0].toInteger()
            sh(label: "temp json removal", script: "rm ${jsonFileName}", returnStatus: true) // Requires Jenkins 'sh' step
            return result
        }
        else {
            echo("Image ${imageWithTag} does not exist at server ${server}")
            sh(label: "temp json removal", script: "rm ${jsonFileName}", returnStatus: true) // Requires Jenkins 'sh' step
            return result
        }
    }
    catch (Exception e) {
        echo 'Exception occurred during image existence check: ' + e.toString()
        echo('Aborting the build due to image check failure.')
        // These steps are specific to Jenkins pipeline error handling
        currentBuild.result = 'ABORTED'
        error(e.toString())
        sh(label: "temp json removal", script: "rm ${jsonFileName}", returnStatus: true) // Clean up even on error
        return [ status : false, imageSize : 0] // Ensure a map is always returned
    }
}