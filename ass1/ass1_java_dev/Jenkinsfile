pipeline {
    agent any

    environment {
        // Repository & Private Registry Hooks
        GITHUB_CREDENTIALS_ID   = 'github-private-repo-token'
        REGISTRY_CREDENTIALS_ID = 'private-docker-registry-auth'
        REGISTRY_URL            = 'your-private-registry.com'
        IMAGE_NAME              = 'ass1-java-dev-app'
        
        // Traceability Tags
        IMAGE_TAG               = "build-${BUILD_NUMBER}"
        SLACK_CHANNEL           = '#devops-alerts'
        
        // Readiness Variables 
        HEALTH_ENDPOINT         = 'http://localhost:8080/actuator/health'
        MAX_RETRIES             = '12' // 12 * 5 seconds = 1 minute total retry window
    }

    options {
        timeout(time: 15, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
        disableConcurrentBuilds()
    }

    stages {
        stage('SCM Pull') {
            steps {
                echo 'Executing Secure Git Source Acquisition...'
                checkout scm
            }
        }

        stage('Install Dependencies and Run Tests') {
            steps {
                dir('ass1_java_dev') {
                    echo 'Running Application Unit Tests...'
                    sh 'mvn clean test'
                }
            }
        }

        stage('Build Multi-stage Image') {
            steps {
                dir('ass1_java_dev') {
                    script {
                        docker.withRegistry("https://${REGISTRY_URL}", REGISTRY_CREDENTIALS_ID) {
                            echo "Building using optimized BuildKit caching layers..."
                            sh """
                                export DOCKER_BUILDKIT=1
                                docker build \
                                  --cache-from ${REGISTRY_URL}/${IMAGE_NAME}:latest \
                                  -t ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG} \
                                  -t ${REGISTRY_URL}/${IMAGE_NAME}:latest .
                            """
                            echo "Pushing traced images to the Private Registry..."
                            sh "docker push ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG}"
                            sh "docker push ${REGISTRY_URL}/${IMAGE_NAME}:latest"
                        }
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                script {
                    echo "Deploying Container via Docker Compose..."
                    // Record existing dynamic version to enable precise rollback if health check fails
                    env.PREVIOUS_TAG = sh(script: "docker inspect --format='{{.Config.Image}}' production_java_container 2>/dev/null | awk -F':' '{print \$2}' || echo 'none'", returnStdout: true).trim()
                    
                    sh "IMAGE_TAG=${IMAGE_TAG} REGISTRY_URL=${REGISTRY_URL} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d --force-recreate"
                }
            }
        }

        stage('Curl Health Endpoint Verification') {
            steps {
                script {
                    echo "Initiating Loop Readiness Polling against ${HEALTH_ENDPOINT}"
                    int retries = 0
                    boolean isHealthy = false
                    int maxRetries = Integer.parseInt(env.MAX_RETRIES)

                    while (retries < maxRetries && !isHealthy) {
                        int statusCode = sh(script: "curl -s -o /dev/null -w '%{http_code}' ${HEALTH_ENDPOINT}", returnStdout: true).trim().toInteger()
                        if (statusCode == 200) {
                            echo "Application verified healthy!"
                            isHealthy = true
                        } else {
                            echo "Endpoint returned code [${statusCode}]. Retrying in 5 seconds..."
                            sleep 5
                            retries++
                        }
                    }

                    if (!isHealthy) {
                        error "Application failed active health validation check."
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                // If application fails to boot properly, instantly rollback to the older running tag
                if (env.PREVIOUS_TAG && env.PREVIOUS_TAG != "none") {
                    echo "CRITICAL FAILURE: Initiating automated rollback to structural tag: ${env.PREVIOUS_TAG}"
                    sh "IMAGE_TAG=${env.PREVIOUS_TAG} REGISTRY_URL=${REGISTRY_URL} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d --force-recreate"
                    slackSend(channel: env.SLACK_CHANNEL, color: '#FF0000', message: "DEPLOYMENT FAILED: Build #${BUILD_NUMBER} failed health validation. Rolling back to safely active Tag: ${env.PREVIOUS_TAG}.")
                } else {
                    slackSend(channel: env.SLACK_CHANNEL, color: '#FF0000', message: "DEPLOYMENT FAILED: Build #${BUILD_NUMBER} collapsed, no stable context tag found for rollback recovery.")
                }
            }
        }
        success {
            slackSend(channel: env.SLACK_CHANNEL, color: '#00FF00', message: "DEPLOYMENT SUCCESSFUL: Version build-${BUILD_NUMBER} is serving production traffic.")
        }
        always {
            echo "Executing Dangling Workspace and Resource Pruning..."
            sh "docker image prune -f"
            cleanWs()
        }
    }
}

