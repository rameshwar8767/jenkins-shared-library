/**
 * clone.groovy
 * Check out Git repository with support for branch, credentials, shallow cloning, and clean checkout.
 *
 * Usage:
 *   clone('https://github.com/org/repo.git')
 *   clone('https://github.com/org/repo.git', 'dev')
 *   clone('https://github.com/org/repo.git', 'main', 'github-credentials')
 *   clone(url: 'https://github.com/org/repo.git', branch: 'main', credentialsId: 'github-creds', shallow: true)
 */
def call(String url, String branch = 'main', String credentialsId = null) {
    call(url: url, branch: branch, credentialsId: credentialsId)
}

def call(Map config = [:]) {
    def url = config.url ?: error("Git repository URL ('url') is required")
    def branch = config.branch ?: 'main'
    def credentialsId = config.credentialsId ?: config.credentials ?: null
    def shallow = config.get('shallow', false)
    def depth = config.get('depth', 1)
    def clean = config.get('clean', true)

    echo "Checking out Git repository: ${url} (branch: ${branch})"

    if (shallow || clean) {
        checkout([
            $class: 'GitSCM',
            branches: [[name: "*/${branch}"]],
            doGenerateSubmoduleConfigurations: false,
            extensions: [
                clean ? [$class: 'CleanCheckout'] : null,
                shallow ? [$class: 'CloneOption', depth: depth, noTags: false, reference: '', shallow: true] : null
            ].findAll { it != null },
            userRemoteConfigs: [[
                url: url,
                credentialsId: credentialsId
            ]]
        ])
    } else {
        if (credentialsId) {
            git url: url, branch: branch, credentialsId: credentialsId
        } else {
            git url: url, branch: branch
        }
    }
}
