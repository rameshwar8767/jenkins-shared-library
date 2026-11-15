/**
 * trivy_scan.groovy
 * Scans filesystem, docker images, repositories, or configs for security vulnerabilities
 * with configurable severity, exit codes, output formats, and artifact archiving.
 *
 * Usage:
 *   trivy_scan()                                                 // Scans filesystem '.'
 *   trivy_scan('my-image:latest')                                // Scans image
 *   trivy_scan(scanType: 'image', target: 'my-org/app:v1.0.0', severity: 'CRITICAL', exitCode: 1)
 *   trivy_scan(scanType: 'fs', target: '.', severity: 'HIGH,CRITICAL', exitCode: 0)
 */
def call(String target = '.') {
    call(target: target)
}

def call(Map config = [:]) {
    def scanType = config.scanType ?: (config.image ? 'image' : (config.target && config.target.contains(':') ? 'image' : 'fs'))
    def target = config.target ?: config.image ?: '.'
    def severity = config.severity ?: 'HIGH,CRITICAL'
    def exitCode = config.exitCode != null ? config.exitCode : 0
    def format = config.format ?: 'table'
    def outputFile = config.outputFile ?: "trivy-${scanType}-report.txt"
    def reportDir = config.reportDir ?: 'reports'
    def ignoreUnfixed = config.get('ignoreUnfixed', true)
    def archiveReport = config.get('archiveReport', true)
    def extraArgs = config.extraArgs ?: ''

    echo "Running Trivy ${scanType} security scan on '${target}' (severity: ${severity}, exitCode: ${exitCode})..."

    sh "mkdir -p ${reportDir}"
    def reportPath = "${reportDir}/${outputFile}"

    def cmd = [
        'trivy',
        scanType,
        "--severity ${severity}",
        "--exit-code ${exitCode}",
        ignoreUnfixed ? '--ignore-unfixed' : '',
        "--format ${format}",
        "--output ${reportPath}",
        extraArgs,
        target
    ].findAll { it?.trim() }.join(' ')

    try {
        sh cmd
        echo "Trivy scan passed successfully. Report saved to ${reportPath}"
    } catch (Exception e) {
        echo "Trivy scan detected vulnerabilities exceeding threshold or failed: ${e.getMessage()}"
        throw e
    } finally {
        if (archiveReport) {
            archiveArtifacts artifacts: reportPath, allowEmptyArchive: true
        }
    }
}
