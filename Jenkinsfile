#!groovy
@Library('visibilityLibs')
import com.liaison.jenkins.visibility.Utilities
import com.liaison.jenkins.common.kubernetes.*

def utils = new Utilities()
def k8sDocker = new Docker()

timestamps {
    node('agent-gcloud') {
        stage('Checkout') {
            timeout(10) {
                def scmVars = checkout scm
                env.GIT_COMMIT = scmVars.GIT_COMMIT
                env.GIT_URL = scmVars.GIT_URL
                env.REPO_NAME = utils.runSh("basename -s .git ${env.GIT_URL}")
                env.DOCKER_IMAGE_NAME = "google-cloud-mockserver"
                env.PROJECT_VERSION = utils.runSh("sed -nE 's/##.*\\[([0-9]+\\.[0-9]+\\.[0-9]+)\\].*/\\1/p' CHANGELOG.md | head -n1")
                env.DOCKER_IMAGE_TAG = "gcr.io/seventh-chassis-87509/${env.DOCKER_IMAGE_NAME}:${env.PROJECT_VERSION}"
                env.DOCKER_IMAGE_TAG_LATEST = "gcr.io/seventh-chassis-87509/${env.DOCKER_IMAGE_NAME}:latest"
                env.SERVICE_ACCOUNT = "jenkins-pipeline@seventh-chassis-87509.iam.gserviceaccount.com"
                env.DOCKER_REGISTRY_URL = "https://gcr.io"
                currentBuild.displayName = env.DOCKER_IMAGE_TAG
            }
        }

        stage('Build Docker image') {
            timeout(10) {
                k8sDocker.build(imageName: env.DOCKER_IMAGE_NAME)
            }
        }


        if (utils.isMasterBuild()) {    
            stage('Push Docker image to GC registry') {
                timeout(10) {
                    withCredentials([usernamePassword(credentialsId: 'google-cloud-service-account', usernameVariable: 'USERNAME', passwordVariable: 'SECRET_JSON')]) {
                        // Authenticate with GC registry.
                        sh "echo '${SECRET_JSON}' | docker login -u _json_key --password-stdin ${env.DOCKER_REGISTRY_URL}"

                        // Tag with version number and push to Google Cloud Docker registry
                        sh "docker tag ${env.DOCKER_IMAGE_NAME} ${env.DOCKER_IMAGE_TAG}"                        
                        sh "docker push ${env.DOCKER_IMAGE_TAG}"

                        // Tag with "latest" and push to Google Cloud Docker registry
                        sh "docker tag ${env.DOCKER_IMAGE_NAME} ${env.DOCKER_IMAGE_TAG_LATEST}"                        
                        sh "docker push ${env.DOCKER_IMAGE_TAG_LATEST}"

                        }
                }
            }

            stage('Deploy Mockserver to GC skynet') {
                timeout(10) {
                    withCredentials([usernamePassword(credentialsId: 'google-cloud-service-account', usernameVariable: 'USERNAME', passwordVariable: 'SECRET_JSON')]) {
                        //Create GC json file from credentials
                        sh 'echo "${SECRET_JSON}" > gc.json'
                        sh "gcloud auth activate-service-account ${SERVICE_ACCOUNT} --key-file=gc.json --project=seventh-chassis-87509"
                        sh "gcloud container clusters get-credentials mock-servers-cluster --zone=us-central1-a --project=seventh-chassis-87509"
                        sh "kubectl apply -f K8sfile.yaml -n default"
                    }
                }
            }
        }
    }

}