/**
 * run_tests.groovy
 * Executes unit and integration tests with automatic build-tool detection (Maven, npm, pytest, Go, Gradle),
 * configurable commands, optional test result archiving (JUnit), and failure tolerance.
 *
 * Usage:
 *   run_tests()                                   // Auto-detects pom.xml, package.json, pytest, etc.
 *   run_tests('npm run test:ci')                  // Custom command string
 *   run_tests(command: 'mvn test', testResults: '**/surefire-reports/*.xml', ignoreFailures: false)
 */
def call(String command) {
    call(command: command)
}

def call(Map config = [:]) {
    def command = config.command ?: config.cmd ?: null
    def testResults = config.testResults ?: null
    def ignoreFailures = config.get('ignoreFailures', false)

    echo "Preparing test execution..."

    if (!command) {
        // Auto-detect project type based on build descriptor files
        if (fileExists('pom.xml')) {
            echo "Detected Maven project (pom.xml). Running mvn test..."
            command = 'mvn clean test'
            if (!testResults) testResults = '**/target/surefire-reports/*.xml'
        } else if (fileExists('package.json')) {
            echo "Detected Node.js project (package.json). Running npm test..."
            command = 'npm test'
        } else if (fileExists('pytest.ini') || fileExists('requirements.txt')) {
            echo "Detected Python project. Running pytest..."
            command = 'pytest --junitxml=reports/junit.xml || pytest'
            if (!testResults) testResults = 'reports/junit.xml'
        } else if (fileExists('go.mod')) {
            echo "Detected Go project (go.mod). Running go test..."
            command = 'go test -v ./...'
        } else if (fileExists('build.gradle') || fileExists('build.gradle.kts')) {
            echo "Detected Gradle project. Running gradle test..."
            command = './gradlew test'
            if (!testResults) testResults = '**/build/test-results/test/*.xml'
        } else {
            echo "Notice: No recognized build file found and no command specified. Pass command: 'your-test-command'."
            return
        }
    }

    echo "Executing test command: ${command}"
    try {
        sh command
        echo "Tests completed successfully."
    } catch (Exception e) {
        if (ignoreFailures) {
            echo "Warning: Tests encountered failures, but ignoreFailures is enabled: ${e.getMessage()}"
            currentBuild.result = 'UNSTABLE'
        } else {
            throw e
        }
    } finally {
        if (testResults) {
            try {
                junit testResults: testResults, allowEmptyResults: true
            } catch (Exception ex) {
                echo "Notice: JUnit results archiving skipped or completed: ${ex.getMessage()}"
            }
        }
    }
}
