/**
 * docker_build.groovy
 * Builds Docker images with support for multiple tags, build arguments, dockerfile, and context.
 *
 * Usage:
 *   docker_build('my-app', 'v1.0.0')
 *   docker_build(imageName: 'my-app', imageTag: 'v1.0.0', dockerfile: 'Dockerfile.prod')
 *   docker_build(imageName: 'my-app', imageTag: 'v1.0.0', buildArgs: [ENV: 'production'], tagLatest: false)
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
    def dockerfile = config.dockerfile ?: 'Dockerfile'
    def context = config.context ?: '.'
    def tagLatest = config.get('tagLatest', true)
    def buildArgs = config.buildArgs ?: [:]
    def additionalTags = config.additionalTags ?: []
    def noCache = config.get('noCache', false)
    def pull = config.get('pull', false)
    def target = config.target ?: null

    echo "Building Docker image: ${imageName}:${imageTag} using ${dockerfile}"

    // Build tags
    def tagsList = ["-t ${imageName}:${imageTag}"]
    if (tagLatest && imageTag != 'latest') {
        tagsList << "-t ${imageName}:latest"
    }
    additionalTags.each { extraTag ->
        tagsList << "-t ${imageName}:${extraTag}"
    }

    // Build args
    def argsList = []
    buildArgs.each { key, val ->
        argsList << "--build-arg ${key}=${val}"
    }

    // Flags
    def flags = []
    if (noCache) flags << "--no-cache"
    if (pull) flags << "--pull"
    if (target) flags << "--target ${target}"

    def buildCmd = [
        'docker build',
        tagsList.join(' '),
        flags.join(' '),
        argsList.join(' '),
        "-f ${dockerfile}",
        "${context}"
    ].findAll { it?.trim() }.join(' ')

    sh buildCmd
    return "${imageName}:${imageTag}"
}
