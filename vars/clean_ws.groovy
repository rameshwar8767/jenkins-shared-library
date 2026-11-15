/**
 * clean_ws.groovy
 * Cleans the Jenkins agent workspace with configurable options and error resilience.
 *
 * Usage:
 *   clean_ws()
 *   clean_ws(deleteDirs: true, notFailBuild: true)
 *   clean_ws(patterns: [[pattern: '.git/**', type: 'EXCLUDE']])
 */
def call(Map config = [:]) {
    def cleanParams = [:]

    // Map supported Jenkins WsCleanup plugin parameters cleanly
    if (config.containsKey('deleteDirs')) cleanParams.deleteDirs = config.deleteDirs
    if (config.containsKey('notFailBuild')) cleanParams.notFailBuild = config.notFailBuild
    if (config.containsKey('cleanWhenSuccess')) cleanParams.cleanWhenSuccess = config.cleanWhenSuccess
    if (config.containsKey('cleanWhenFailure')) cleanParams.cleanWhenFailure = config.cleanWhenFailure
    if (config.containsKey('cleanWhenAborted')) cleanParams.cleanWhenAborted = config.cleanWhenAborted
    if (config.containsKey('cleanWhenUnstable')) cleanParams.cleanWhenUnstable = config.cleanWhenUnstable
    if (config.containsKey('disableDeferredWipeout')) cleanParams.disableDeferredWipeout = config.disableDeferredWipeout
    if (config.patterns) cleanParams.patterns = config.patterns

    echo "Cleaning up workspace..."
    try {
        if (cleanParams) {
            cleanWs(cleanParams)
        } else {
            cleanWs()
        }
        echo "Workspace cleaned successfully."
    } catch (Exception e) {
        if (config.get('notFailBuild', true)) {
            echo "Warning: Workspace cleanup encountered an issue: ${e.getMessage()}"
        } else {
            throw e
        }
    }
}
