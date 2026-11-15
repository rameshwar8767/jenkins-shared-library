/**
 * generate_reports.groovy
 * Generates structured build and deployment reports and archives them as Jenkins artifacts.
 *
 * Usage:
 *   generate_reports()
 *   generate_reports(projectName: 'MyService', imageName: 'my-org/my-service', imageTag: 'v1.2.0')
 *   generate_reports(projectName: 'PaymentAPI', extraInfo: [Environment: 'Staging', Tester: 'QA Team'])
 */
def call(Map config = [:]) {
    def projectName = config.projectName ?: 'Project'
    def imageName = config.imageName ?: 'N/A'
    def imageTag = config.imageTag ?: 'N/A'
    def reportDir = config.reportDir ?: 'reports'
    def reportFile = config.reportFile ?: 'build-report.txt'
    def archive = config.get('archive', true)
    def extraInfo = config.extraInfo ?: [:]

    // Accurately capture build status in Jenkins pipeline
    def buildStatus = currentBuild.currentResult ?: currentBuild.result ?: 'SUCCESS'
    def buildNumber = env.BUILD_NUMBER ?: 'Local'
    def buildUrl = env.BUILD_URL ?: 'N/A'
    def gitBranch = env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'N/A'
    def gitCommit = env.GIT_COMMIT ?: 'N/A'
    def nodeName = env.NODE_NAME ?: 'Jenkins Agent'

    echo "Generating build report for ${projectName}..."

    sh "mkdir -p ${reportDir}"

    def reportPath = "${reportDir}/${reportFile}"
    def reportContent = """
==================================================
              ${projectName} Build Report
==================================================
Generated At : \$(date -u 2>/dev/null || date)
Build Status : ${buildStatus}
Build Number : ${buildNumber}
Build URL    : ${buildUrl}
Node Name    : ${nodeName}
Git Branch   : ${gitBranch}
Git Commit   : ${gitCommit}
Docker Image : ${imageName}
Image Tag    : ${imageTag}
"""
    if (extraInfo) {
        reportContent += "\n--- Additional Metadata ---\n"
        extraInfo.each { k, v ->
            reportContent += "${k} : ${v}\n"
        }
    }
    reportContent += "==================================================\n"

    writeFile file: reportPath, text: reportContent

    if (archive) {
        archiveArtifacts artifacts: "${reportDir}/*", allowEmptyArchive: true
    }

    echo "Report successfully generated and archived at ${reportPath}"
}
