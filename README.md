# Jenkins Shared Libraries

A reusable, modular, and production-ready Jenkins Shared Library for modern CI/CD pipelines.

---

## 📁 Repository Structure

```text
jenkins-shared-libraries/
├── vars/
│   ├── clean_ws.groovy             # Workspace cleanup with fail-safe handling
│   ├── clone.groovy                # Git checkout supporting credentials, branch, depth
│   ├── docker_build.groovy         # Docker build with multiple tags and build-args
│   ├── docker_push.groovy          # Docker push with registry support and safe logout
│   ├── generate_reports.groovy     # Build report generator & artifact archiver
│   ├── hello.groovy                # Custom greeting & diagnostics utility
│   ├── run_tests.groovy            # Auto-detecting test runner (Maven, npm, pytest, Go, Gradle)
│   ├── trivy_scan.groovy           # Trivy vulnerability scanner (FS, Image, Repo)
│   └── update_k8s_manifests.groovy # GitOps K8s manifest image tag updater & pusher
└── README.md
```

---

## 🚀 How to Configure in Jenkins

1. Go to **Manage Jenkins** ➔ **System** ➔ **Global Pipeline Libraries**.
2. Click **Add**:
   - **Name**: `jenkins-shared-libraries` (or any library identifier).
   - **Default version**: `main` (or branch/tag of your choice).
   - **Retrieval method**: Modern SCM (Git).
   - **Project Repository**: URL of this repository.
   - **Credentials**: Jenkins credentials with access to this repo.
3. In your `Jenkinsfile`, import the library:
   ```groovy
   @Library('jenkins-shared-libraries@main') _
   ```

---

## 📖 Step Reference & Usage

### 1. `clean_ws`
Cleans up the Jenkins workspace safely with error resilience.

```groovy
// Clean with defaults
clean_ws()

// Custom configuration
clean_ws(
    deleteDirs: true,
    notFailBuild: true,
    cleanWhenSuccess: true,
    patterns: [[pattern: '.git/**', type: 'EXCLUDE']]
)
```

---

### 2. `clone`
Clones or checks out a Git repository. Supports positional arguments or named parameters, shallow cloning, and credentials.

```groovy
// Positional signatures
clone('https://github.com/my-org/my-repo.git')
clone('https://github.com/my-org/my-repo.git', 'develop')
clone('https://github.com/my-org/my-repo.git', 'main', 'github-credentials')

// Named parameters
clone(
    url: 'https://github.com/my-org/my-repo.git',
    branch: 'release/v1.0',
    credentialsId: 'github-credentials',
    shallow: true,
    depth: 1,
    clean: true
)
```

---

### 3. `docker_build`
Builds Docker images with support for multiple tags, build arguments, custom Dockerfiles, and caching controls.

```groovy
// Simple build
docker_build('myorg/myapp', 'v1.0.0')

// Advanced build
docker_build(
    imageName: 'myorg/myapp',
    imageTag: 'v1.0.0',
    dockerfile: 'Dockerfile.production',
    context: '.',
    tagLatest: true,
    buildArgs: [
        NODE_ENV: 'production',
        APP_PORT: '8080'
    ],
    additionalTags: ['latest', "${env.BUILD_NUMBER}"],
    noCache: false,
    pull: true
)
```

---

### 4. `docker_push`
Pushes Docker images to Docker Hub or any private container registry (AWS ECR, GCP Artifact Registry, Azure ACR, Harbor, GHCR) with guaranteed credential cleanup/logout.

```groovy
// Docker Hub push
docker_push('myorg/myapp', 'v1.0.0')

// Docker Hub with custom credentials and latest tag
docker_push(
    imageName: 'myorg/myapp',
    imageTag: 'v1.0.0',
    credentialsId: 'docker-hub-credentials',
    pushLatest: true
)

// Private registry (e.g. GitHub Container Registry or AWS ECR)
docker_push(
    imageName: 'ghcr.io/myorg/myapp',
    imageTag: 'v1.0.0',
    registry: 'ghcr.io',
    credentialsId: 'ghcr-token',
    pushLatest: true
)
```

---

### 5. `generate_reports`
Generates build status summaries (including Git commit, branch, build number, node name, docker tags) and automatically archives them as build artifacts.

```groovy
// Basic report
generate_reports()

// Customized report with metadata
generate_reports(
    projectName: 'PaymentGateway',
    imageName: 'myorg/payment-service',
    imageTag: 'v1.2.3',
    reportDir: 'build-artifacts',
    reportFile: 'deployment-summary.txt',
    archive: true,
    extraInfo: [
        Environment: 'Staging',
        DeployedBy: 'Jenkins Automation',
        ReleaseNotes: 'Sprint 42 feature batch'
    ]
)
```

---

### 6. `hello`
Diagnostic greeting step.

```groovy
hello()
hello('Platform Team')
hello(name: 'DevOps', message: 'Pipeline initialized successfully!')
```

---

### 7. `run_tests`
Executes test suites with automatic framework detection (`pom.xml` ➔ Maven, `package.json` ➔ npm, Python ➔ pytest, Go ➔ `go test`, Gradle ➔ `./gradlew test`), optional JUnit result archiving, and failure tolerance.

```groovy
// Auto-detect project type and run tests
run_tests()

// Custom test command
run_tests('npm run test:ci')

// Advanced configuration
run_tests(
    command: 'mvn test -DskipITs=false',
    testResults: '**/target/surefire-reports/*.xml',
    ignoreFailures: false
)
```

---

### 8. `trivy_scan`
Security vulnerability scanning using Trivy for filesystems, container images, git repositories, or configs. Automatically exports and archives the report.

```groovy
// Scan local workspace filesystem
trivy_scan()

// Scan container image
trivy_scan('myorg/myapp:v1.0.0')

// Gatekeeping scan (fails build if HIGH or CRITICAL vulnerabilities found)
trivy_scan(
    scanType: 'image',
    target: 'myorg/myapp:v1.0.0',
    severity: 'HIGH,CRITICAL',
    exitCode: 1,
    format: 'table',
    outputFile: 'trivy-image-vulns.txt',
    ignoreUnfixed: true,
    archiveReport: true
)
```

---

### 9. `update_k8s_manifests`
GitOps step that updates Kubernetes deployment YAMLs with newly built image tags and pushes changes back to Git without hardcoded repos or credential leaks.

```groovy
update_k8s_manifests(
    imageTag: 'v1.0.0',
    imageName: 'myorg/myapp',
    manifestsPath: 'k8s',
    gitBranch: 'main',
    gitCredentials: 'github-credentials',
    commitMessage: 'ci: update application deployment to v1.0.0 [ci skip]'
)
```

---

## 🌟 Complete End-to-End Jenkinsfile Example

```groovy
@Library('jenkins-shared-libraries@main') _

pipeline {
    agent any

    environment {
        APP_NAME         = 'ecommerce-api'
        IMAGE_NAME       = "myorg/${APP_NAME}"
        IMAGE_TAG        = "v1.0.${env.BUILD_NUMBER}"
        DOCKER_CREDS     = 'docker-hub-credentials'
        GITHUB_CREDS     = 'github-credentials'
    }

    options {
        timeout(time: 1, unit: 'HOURS')
        disableConcurrentBuilds()
    }

    stages {
        stage('Initialize') {
            steps {
                hello('Cloud Architecture Team')
                clean_ws()
            }
        }

        stage('Checkout') {
            steps {
                clone(
                    url: 'https://github.com/myorg/ecommerce-api.git',
                    branch: env.BRANCH_NAME ?: 'main',
                    credentialsId: env.GITHUB_CREDS,
                    shallow: true
                )
            }
        }

        stage('Run Tests') {
            steps {
                run_tests()
            }
        }

        stage('Filesystem Security Scan') {
            steps {
                trivy_scan(
                    scanType: 'fs',
                    target: '.',
                    severity: 'CRITICAL',
                    exitCode: 0
                )
            }
        }

        stage('Build Docker Image') {
            steps {
                docker_build(
                    imageName: env.IMAGE_NAME,
                    imageTag: env.IMAGE_TAG,
                    dockerfile: 'Dockerfile',
                    tagLatest: true
                )
            }
        }

        stage('Container Image Scan') {
            steps {
                trivy_scan(
                    scanType: 'image',
                    target: "${env.IMAGE_NAME}:${env.IMAGE_TAG}",
                    severity: 'HIGH,CRITICAL',
                    exitCode: 0
                )
            }
        }

        stage('Push Docker Image') {
            steps {
                docker_push(
                    imageName: env.IMAGE_NAME,
                    imageTag: env.IMAGE_TAG,
                    credentialsId: env.DOCKER_CREDS,
                    pushLatest: true
                )
            }
        }

        stage('Update K8s Manifests') {
            steps {
                update_k8s_manifests(
                    imageName: env.IMAGE_NAME,
                    imageTag: env.IMAGE_TAG,
                    manifestsPath: 'k8s',
                    gitCredentials: env.GITHUB_CREDS
                )
            }
        }
    }

    post {
        always {
            generate_reports(
                projectName: env.APP_NAME,
                imageName: env.IMAGE_NAME,
                imageTag: env.IMAGE_TAG
            )
            clean_ws(notFailBuild: true)
        }
    }
}
```
