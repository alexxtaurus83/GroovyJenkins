import groovy.json.JsonSlurper

// Parameters:
//   buildId (Integer): The ID of the current Jenkins build.
//   debug (Boolean): If true, enables verbose logging.
//
// Returns:
//   boolean: True if no leaks are found, false if leaks are detected.
boolean call(Integer buildId, Boolean debug = false) {
    def leakCount = 0
    String gitleaksFileContent // Renamed to avoid confusion with file path
    List gitleaksJsonOutput
   
    def tomlFile = libraryResource 'configuration/.gitleaks.toml'
    writeFile file: ".gitleaks.toml", text: tomlFile

    // Execute gitleaks.
    // -s ./ : Scan current directory
    // -v : Verbose output
    // --redact : Redact secrets in output
    // --no-git : Do not use git history, scan current working directory
    // -r=gitleaksoutput.json : Report results to JSON file
    // The stdout is also redirected to the JSON file, which might be redundant or intended.
    // Requires Jenkins 'sh' step.
    def status = sh(label: "gitleaks", script: "gitleaks detect -s ./ -v --redact --no-git -r=gitleaksoutput.json > gitleaksoutput.json", returnStatus: true)

    boolean leaksFound = false
    if (status != 0) { // gitleaks exits with non-zero status if leaks are found
        leaksFound = true
        echo 'Leaks detected, full details can be seen below'
        // Read and display the gitleaks JSON output
        // Requires Jenkins 'readFile' step.
        gitleaksFileContent = readFile file: "gitleaksoutput.json"
        echo "${gitleaksFileContent}"
        // Parse the JSON output
        gitleaksJsonOutput = readJSON text: gitleaksFileContent, returnPojo: true
        leakCount = gitleaksJsonOutput.size()

        // Clean up the temporary gitleaks output file
        sh "rm gitleaksoutput.json"
    }
    echo "Leak count: ${leakCount}"

    // Prepare metrics data for reporting
    def gitleaksMetric = [:]
    gitleaksMetric["buildId"] = buildId
    gitleaksMetric["leaks"] = leakCount
    if (leaksFound) {
        gitleaksMetric["data"] = gitleaksFileContent // Include full leak data if found
    }

    return !leaksFound // Return true if no leaks, false if leaks were found
}