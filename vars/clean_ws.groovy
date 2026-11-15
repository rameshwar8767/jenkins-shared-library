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
    boolean deleteDirs = config.get('deleteDirs', true)
    boolean notFailBuild = config.get('notFailBuild', true)
    boolean cleanWhenSuccess = config.get('cleanWhenSuccess', true)
    boolean cleanWhenNotSuccess = config.get('cleanWhenNotSuccess', true)
    List patterns = config.get('patterns', [])

    echo "Cleaning up workspace..."
    try {
        if (patterns) {
            cleanWs(
                cleanWhenSuccess: cleanWhenSuccess,
                cleanWhenNotSuccess: cleanWhenNotSuccess,
                deleteDirs: deleteDirs,
                notFailBuild: notFailBuild,
                patterns: patterns
            )
        } else {
            cleanWs(
                cleanWhenSuccess: cleanWhenSuccess,
                cleanWhenNotSuccess: cleanWhenNotSuccess,
                deleteDirs: deleteDirs,
                notFailBuild: notFailBuild
            )
        }
        echo "Workspace cleaned successfully."
    } catch (Exception e) {
        if (notFailBuild) {
            echo "Warning: Workspace cleanup encountered an issue: ${e.getMessage()}"
        } else {
            throw e
        }
    }
}
