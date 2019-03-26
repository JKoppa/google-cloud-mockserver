#!groovy
@Library('visibilityLibs')
import com.liaison.jenkins.visibility.Utilities
import com.liaison.jenkins.common.kubernetes.*

def utils = new Utilities()
def k8sDocker = new Docker()

timestamps {
    node() {
        stage('Checkout') {
            timeout(10) {
                def scmVars = checkout scm
                env.GIT_COMMIT = scmVars.GIT_COMMIT
                env.GIT_URL = scmVars.GIT_URL
                env.REPO_NAME = utils.runSh("basename -s .git ${env.GIT_URL}")
                env.DOCKER_IMAGE_NAME = "google-cloud-mockserver"
                env.PROJECT_VERSION = utils.runSh("sed -nE 's/##.*\\[([0-9]+\\.[0-9]+\\.[0-9]+)\\].*/\\1/p' CHANGELOG.md | head -n1")
                env.DOCKER_IMAGE_TAG = "gcr.io/seventh-chassis-87509/${env.DOCKER_IMAGE_NAME}:${env.PROJECT_VERSION}"
                env.DOCKER_REGISTRY_URL = "https://gcr.io"
                currentBuild.displayName = env.DOCKER_IMAGE_TAG
            }
        }

        stage('Build Docker image') {
            timeout(10) {
                k8sDocker.build(imageName: env.DOCKER_IMAGE_NAME)
            }
        }


        //if (utils.isMasterBuild()) {    
            stage('Push Docker image to GC registry') {
                timeout(10) {
                    // Push to Google Cloud Docker registry
                    sh "docker tag ${env.DOCKER_IMAGE_NAME} ${env.DOCKER_IMAGE_TAG}"

                    withCredentials([string(credentialsId: 'google-cloud-service-account', variable: 'SECRET_JSON')]) {
                         
                        // Authenticate with registry.
                        sh "echo '${SECRET_JSON}' | docker login -u _json_key --password-stdin ${env.DOCKER_REGISTRY_URL}"

                        // Publish image to GC registry.
                        sh "docker push ${env.DOCKER_IMAGE_TAG}"

                    }
                }
            }
        //}

     
    }

}