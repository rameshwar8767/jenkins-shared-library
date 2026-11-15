#!/usr/bin/env groovy

/**
 * update_k8s_manifests.groovy
 * Updates Kubernetes manifest YAML files with a new image tag and commits/pushes
 * the changes back to the Git repository.
 *
 * Usage:
 *   update_k8s_manifests(
 *       imageTag: 'v1.0.0',
 *       imageName: 'myorg/myapp',
 *       manifestsPath: 'k8s',
 *       gitBranch: 'main'
 *   )
 *   // Or with multiple images:
 *   update_k8s_manifests(
 *       imageTag: 'v1.0.0',
 *       images: ['myorg/myapp', 'myorg/migration'],
 *       manifestsPath: 'k8s'
 *   )
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag ('imageTag') is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: config.credentialsId ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def rawBranch = config.gitBranch ?: env.BRANCH_NAME ?: env.GIT_BRANCH ?: 'main'
    // Normalize branch name (strip origin/ or refs/heads/)
    def gitBranch = rawBranch.replaceAll('^origin/', '').replaceAll('^refs/heads/', '')
    def gitRepoUrl = config.gitRepoUrl ?: config.repoUrl ?: null
    def commitMessage = config.commitMessage ?: "Update Kubernetes manifests with image tag ${imageTag} [ci skip]"
    def manifestFiles = config.manifestFiles ?: []

    // Collect targeted image names (supports single imageName or list of images)
    def targetImages = []
    if (config.images) {
        if (config.images instanceof List) {
            targetImages = config.images
        } else {
            targetImages = [config.images.toString()]
        }
    } else if (config.imageName) {
        targetImages = [config.imageName.toString()]
    }

    echo "Updating Kubernetes manifests in '${manifestsPath}' with image tag: ${imageTag} (branch: ${gitBranch})"

    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // 1. Configure local Git user identity
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """

        // 2. Resolve Git repository remote URL
        def pushRepoUrl = gitRepoUrl
        if (!pushRepoUrl) {
            def currentOrigin = sh(script: 'git config --get remote.origin.url || true', returnStdout: true).trim()
            if (currentOrigin) {
                // Strip existing credentials if embedded in origin URL
                pushRepoUrl = currentOrigin.replaceAll('https://[^@]+@', 'https://')
            } else {
                error("Git repository URL was not provided in config and could not be determined from git remote.")
            }
        }

        // Format authenticated push URL with masked credentials
        def authPushUrl = pushRepoUrl.replaceFirst('^(https?://)', "\$1\${GIT_USERNAME}:\${GIT_PASSWORD}@")

        // 3. Update manifests
        if (manifestFiles && manifestFiles.size() > 0) {
            manifestFiles.each { file ->
                def filePath = "${manifestsPath}/${file}"
                if (targetImages.size() > 0) {
                    targetImages.each { img ->
                        sh """
                            if [ -f "${filePath}" ]; then
                                sed -i "s|image: ${img}:.*|image: ${img}:${imageTag}|g" "${filePath}"
                                echo "Updated ${filePath} with image: ${img}:${imageTag}"
                            else
                                echo "Warning: Manifest file ${filePath} not found."
                            fi
                        """
                    }
                } else {
                    sh """
                        if [ -f "${filePath}" ]; then
                            sed -i -E "s|image: ([a-zA-Z0-9_./-]+):[a-zA-Z0-9_.-]+|image: \\1:${imageTag}|g" "${filePath}"
                            echo "Updated all images in ${filePath} to tag: ${imageTag}"
                        fi
                    """
                }
            }
        } else {
            // General update: check if specific files exist (for backward compatibility), or update all YAMLs
            def imageReplacements = ""
            if (targetImages.size() > 0) {
                imageReplacements = targetImages.collect { img ->
                    """
                    find "${manifestsPath}" -type f \\( -name "*.yaml" -o -name "*.yml" \\) -exec sed -i "s|image: ${img}:.*|image: ${img}:${imageTag}|g" {} +
                    echo "Updated manifests matching image '${img}' to tag '${imageTag}'"
                    """
                }.join('\n')
            } else {
                imageReplacements = """
                    find "${manifestsPath}" -type f \\( -name "*.yaml" -o -name "*.yml" \\) -exec sed -i -E "s|image: ([a-zA-Z0-9_./-]+):[a-zA-Z0-9_.-]+|image: \\1:${imageTag}|g" {} +
                    echo "Updated all image tags across YAML files in '${manifestsPath}' to '${imageTag}'"
                """
            }

            sh """
                if [ -d "${manifestsPath}" ]; then
                    # Backward compatibility for tutorial files if they exist
                    if [ -f "${manifestsPath}/08-easyshop-deployment.yaml" ]; then
                        sed -i "s|image: trainwithshubham/easyshop-app:.*|image: trainwithshubham/easyshop-app:${imageTag}|g" "${manifestsPath}/08-easyshop-deployment.yaml"
                    fi
                    if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                        sed -i "s|image: trainwithshubham/easyshop-migration:.*|image: trainwithshubham/easyshop-migration:${imageTag}|g" "${manifestsPath}/12-migration-job.yaml"
                    fi
                    if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                        sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" "${manifestsPath}/10-ingress.yaml"
                    fi

                    ${imageReplacements}
                else
                    echo "Warning: Manifest directory '${manifestsPath}' does not exist in workspace."
                fi
            """
        }

        // 4. Check for changes, commit, and push
        sh """
            if git diff --quiet "${manifestsPath}"; then
                echo "No manifest changes detected in '${manifestsPath}'. Skipping commit and push."
            else
                git add "${manifestsPath}"
                git commit -m "${commitMessage}"
                
                # Push changes without persisting authentication secrets into .git/config
                git push "${authPushUrl}" HEAD:"${gitBranch}"
                echo "Successfully committed and pushed manifest updates to branch '${gitBranch}'."
            fi
        """
    }
}
