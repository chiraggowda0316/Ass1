pipeline {
    agent any

    environment {
        // Repository & Private Registry Hooks (Updated for your Docker Hub profile)
        GITHUB_CREDENTIALS_ID   = 'github-private-repo-token'
        REGISTRY_CREDENTIALS_ID = 'private-docker-registry-auth'
        REGISTRY_URL            = 'docker.io'
        IMAGE_NAME              = 'chiraggowda0316/ass1-java-dev-app'
        
        // Traceability Tags
        IMAGE_TAG               = "build-${BUILD_NUMBER}"
        SLACK_CHANNEL           = '#devops-alerts'
        
        // Readiness Variables (Mapped to the exposed host port)
        HEALTH_ENDPOINT         = 'http://localhost:8082/actuator/health'
        MAX_RETRIES             = '15' // Allows a 75-second startup window
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
                        // Securely bind Jenkins credentials to temporary shell environment variables
                        withCredentials([usernamePassword(credentialsId: env.REGISTRY_CREDENTIALS_ID, 
                                                         usernameVariable: 'DOCKER_USER', 
                                                         passwordVariable: 'DOCKER_PASS')]) {
                            
                            echo "Building using optimized BuildKit caching layers..."
                            sh """
                                export DOCKER_BUILDKIT=1
                                docker build \
                                  --cache-from ${REGISTRY_URL}/${IMAGE_NAME}:latest \
                                  -t ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG} \
                                  -t ${REGISTRY_URL}/${IMAGE_NAME}:latest .
                            """
                            
                            echo "Authenticating explicitly against Docker Hub CLI..."
                            sh "echo \$DOCKER_PASS | docker login -u \$DOCKER_USER --password-stdin"
                            
                            echo "Pushing traced images to Docker Hub..."
                            sh "docker push ${REGISTRY_URL}/${IMAGE_NAME}:${IMAGE_TAG}"
                            sh "docker push ${REGISTRY_URL}/${IMAGE_NAME}:latest"
                            
                            echo "Cleaning up local runtime registry session tokens..."
                            sh "docker logout"
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

                    // Initial sleep allowing Tomcat to bind completely before polling
                    sleep 10

                    while (retries < maxRetries && !isHealthy) {
                        int statusCode = sh(script: "curl -s -m 5 -o /dev/null -w '%{http_code}' ${HEALTH_ENDPOINT}", returnStdout: true).trim().toInteger()
                        
                        // Accept HTTP 200 (OK) or security challenges if endpoint is protected
                        if (statusCode == 200 || statusCode == 401 || statusCode == 403) {
                            echo "Application verified alive and listening! Status code: [${statusCode}]"
                            isHealthy = true
                        } else {
                            echo "Endpoint returned code [${statusCode}]. Application still warming up. Retrying in 5 seconds..."
                            sleep 5
                            retries++
                        }
                    }

                    if (!isHealthy) {
                        error "Application failed active health validation check after matching retry limits."
                    }
                }
            }
        }
    }

    post {
        failure {
            script {
                try {
                    // Triggers rollback safely since files are still present in the workspace
                    if (env.PREVIOUS_TAG && env.PREVIOUS_TAG != "none") {
                        echo "CRITICAL FAILURE: Initiating automated rollback to structural tag: ${env.PREVIOUS_TAG}"
                        sh "IMAGE_TAG=${env.PREVIOUS_TAG} REGISTRY_URL=${REGISTRY_URL} IMAGE_NAME=${IMAGE_NAME} docker-compose up -d --force-recreate"
                    } else {
                        echo "No stable context tag found for rollback recovery."
                    }
                } catch (Exception e) {
                    echo "Rollback action failed: ${e.getMessage()}"
                }
            }
        }
        cleanup {
            echo "Executing Dangling Workspace and Resource Pruning..."
            sh "docker image prune -f"
            cleanWs()
        }
    }
}
