// Defines a function to upload a file to a specified URL using HTTP PUT.

// Parameters:
//   fileUrl (String): The URL to which the file will be uploaded.
//   fileName (String): The path to the local file to be uploaded.
//   secretName (String): The Jenkins credentialsId for HTTP authentication (e.g., username/password, token).
//   contentTp (String): The Content-Type header for the HTTP request (e.g., 'APPLICATION_OCTETSTREAM').
//                       Defaults to 'APPLICATION_OCTETSTREAM'.
//   debug (boolean): If true, enables verbose logging.
//
// Returns:
//   int: The HTTP status code of the PUT request (e.g., 201 for created), or 0 if an error occurred
//        or the file already existed and upload was skipped.
//
// Dependencies:
//   - Jenkins Pipeline 'httpRequest', 'echo' steps.
//   - 'remoteFileExists' function (expected to be defined elsewhere in the shared library).
def uploadFile(String fileUrl, String fileName, String secretName, String contentTp = 'APPLICATION_OCTETSTREAM', boolean debug = false) {
    try {
        // Content-Type options: NOT_SET, TEXT_HTML, TEXT_PLAIN, APPLICATION_FORM, APPLICATION_JSON, APPLICATION_JSON_UTF8, APPLICATION_TAR, APPLICATION_ZIP, APPLICATION_OCTETSTREAM
        if (!remoteFileExists(fileUrl, secretName, debug)) {
            echo "${fileUrl} does not exist. Uploading..."
            def responsePost = httpRequest contentType: contentTp,
                                    httpMode: 'PUT', ignoreSslErrors: true, quiet: false, consoleLogResponseBody: true,
                                    multipartName: fileName, timeout: 900, authentication: secretName,
                                    responseHandle: 'NONE', uploadFile: fileName,
                                    url: fileUrl
            return responsePost.status // Typically 201 Created
        }
        else {
            echo "Skip uploading ${fileUrl} since it already exists."
            return 0 // Indicate skip
        }
    }
    catch (Exception e) {
        echo 'Exception occurred during file upload: ' + e.toString()
        return 0 // Indicate error
    }
}

// Defines a function to download a file from a specified URL using HTTP GET.

//
// Parameters:
//   fileUrl (String): The URL of the file to download.
//   secretName (String): The Jenkins credentialsId for HTTP authentication.
//   outFile (String): The path to save the downloaded file. If 'none', the response body
//                     is not written to a file, and only the status is returned.
//   debug (boolean): If true, enables verbose logging.
//
// Returns:
//   int: The HTTP status code of the GET request (e.g., 200 for success, 404 for not found),
//        or 0 if an exception occurred.
//
// Dependencies:
//   - Jenkins Pipeline 'httpRequest', 'echo' steps.
def downloadFile(String fileUrl, String secretName, String outFile = 'none', boolean debug = false) {
    if (debug) {
        echo """
            downloadFile called with:
            fileUrl:    ${fileUrl}
            secretName: (hidden)
            outFile:    ${outFile}
            debug:      ${debug}
        """
    }
    try {
        if (outFile == 'none') {
            def response = httpRequest quiet: !debug, ignoreSslErrors: true, validResponseCodes: "200,404", consoleLogResponseBody: debug, httpMode: 'GET', authentication: secretName, url: fileUrl
            if (debug) {
                echo "no outfile response: ${response}"
            }
            return response.status
        }
        else {
            def response = httpRequest quiet: !debug, ignoreSslErrors: true, validResponseCodes: "200,404", consoleLogResponseBody: debug, httpMode: 'GET', authentication: secretName, outputFile: outFile, url: fileUrl
            if (debug) {
                echo "with outfile response: ${response}"
            }
            return response.status
        }
    }
    catch (Exception e) {
        echo 'Exception occurred during file download: ' + e.toString()
        return 0 // Indicate error
    }
}

// Defines a function to check if a remote file exists using an HTTP HEAD request.

//
// Parameters:
//   fileUrl (String): The URL of the file to check.
//   secretName (String): The Jenkins credentialsId for HTTP authentication.
//   debug (boolean): If true, enables verbose logging.
//
// Returns:
//   boolean: True if the file exists (HTTP status is not 404), false otherwise or on error.
//
// Dependencies:
//   - Jenkins Pipeline 'httpRequest', 'echo' steps.
//   - Assumes 'utils.printStackTrace' (from jenkins_utility_functions.groovy) is available.
boolean remoteFileExists(String fileUrl, String secretName, boolean debug = false) {
    boolean fileExists = false
    try {
        if (debug) {
            echo "remoteFileExists called with fileUrl: ${fileUrl}"
        }
        def response = httpRequest quiet: !debug, ignoreSslErrors: true, validResponseCodes: "200,404", consoleLogResponseBody: debug, httpMode: 'HEAD', authentication: secretName, url: fileUrl
        if (debug) {
            echo "Response: ${response}"
            echo "response headers: ${response.getHeaders()}"
        }
        fileExists = response.status != 404
    }
    catch (Exception e) {
        echo 'Exception occurred during remote file existence check: ' + e.toString()
        if (debug) {
            // Assumes 'utils.printStackTrace' is available from another shared library file
            utils.printStackTrace(e)
        }
        fileExists = false // Ensure false on exception
    }
    return fileExists
}