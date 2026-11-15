/**
 * docker_push.groovy
 * Pushes Docker images to Docker Hub or any private container registry (ECR, GCR, ACR, Harbor)
 * with credential isolation and guaranteed logout cleanup.
 *
 * Usage:
 *   docker_push('my-app', 'v1.0.0')
 *   docker_push(imageName: 'my-app', imageTag: 'v1.0.0', credentialsId: 'docker-hub-credentials')
 *   docker_push(imageName: 'my-app', imageTag: 'v1.0.0', registry: 'ghcr.io', pushLatest: true)
 */
def call(String imageName) {
    call(imageName: imageName, imageTag: 'latest')
}

def call(String imageName, String imageTag) {
    call(imageName: imageName, imageTag: imageTag)
}

def call(Map config = [:]) {
    def imageName = config.imageName ?: error("Docker image name ('imageName') is required")
    def imageTag = config.imageTag ?: 'latest'
    def credentialsId = config.credentialsId ?: config.credentials ?: 'docker-hub-credentials'
    def registry = config.registry ?: ''
    def pushLatest = config.get('pushLatest', false)
    def additionalTags = config.additionalTags ?: []

    echo "Pushing Docker image: ${imageName}:${imageTag}"

    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        usernameVariable: 'DOCKER_USERNAME',
        passwordVariable: 'DOCKER_PASSWORD'
    )]) {
        try {
            def registryArg = registry ? " ${registry}" : ""
            sh """
                echo "\$DOCKER_PASSWORD" | docker login -u "\$DOCKER_USERNAME" --password-stdin${registryArg}
                docker push ${imageName}:${imageTag}
            """

            if (pushLatest && imageTag != 'latest') {
                sh "docker push ${imageName}:latest"
            }

            additionalTags.each { extraTag ->
                sh "docker push ${imageName}:${extraTag}"
            }
        } finally {
            // Always logout to ensure credentials are not retained on agent
            def registryArg = registry ? " ${registry}" : ""
            sh "docker logout${registryArg} || true"
        }
    }
}
