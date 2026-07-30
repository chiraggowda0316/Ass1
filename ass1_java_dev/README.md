Assignment: DevOps Pipeline
Objective
Your assignment is to build out the DevOps CI/CD pipeline and containerization strategy for this  application.

Assignment Details
1. Docker Requirements
You must write a Dockerfile with the following requirements:

Use a multi-stage build to optimize the image size.

Incorporate basic Docker security features (e.g., using a non-root user, minimal base image like alpine, running as a non-root user).

2. Docker Compose Requirements
You must write a docker-compose.yml file to spin up the application easily using docker-compose up.

3. Jenkins Pipeline Requirements
You must write a Jenkinsfile for the CI/CD pipeline. The pipeline should include:

Usage of the environment block to define variables.

The following stages:

SCM Pull: Checkout the code from your repository.

Install Dependencies and Run Tests: 

Build: Build the multi-stage Docker image.

Deploy: Run the application using Docker Compose.

Curl: Verify the deployment by sending a curl request to the health endpoint.


Submission
The URL to your GitHub repository containing the source code along with your new Dockerfile, docker-compose.yml, and Jenkinsfile.

Running deployment.

Important Notes
The deployment must be running and accessible after the pipeline completes.

Older builds and dangling resources must be cleaned up at the end of the pipeline.

Include caching in Docker builds to optimize performance.

Include post notifications in Jenkins (e.g., Slack, email) for build status.

Use a private Docker registry for storing and deploying images.

Use a private Github repo for code store and connect to jenkins.

Implement rollback support by redeploying the previous successful image tag on failure.

Include build number in the image tag for traceability.

Use readiness wait logic instead of fixed sleep before verifying endpoints.

Required Jenkins Plugins
Candidates should ensure the following Jenkins plugins are available:

Docker Pipeline

Slack Notification (or equivalent for post notifications)

Workspace Cleanup

Stage View
