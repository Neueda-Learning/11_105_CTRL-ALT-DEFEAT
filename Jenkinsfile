pipeline {

    agent any

    environment {
        GIT_URL = 'https://github.com/prerna2111/11_105_CTRL-ALT-DEFEAT.git'
        BRANCH = 'main'
    }

    stages {

        stage('Checkout Source') {
            steps {
                git branch: "${BRANCH}",
                    url: "${GIT_URL}"
            }
        }


        stage('Stop Existing Containers') {
            steps {
                sh 'docker-compose down || true'
            }
        }

        

        stage('Build Docker Image') {
            steps {
                sh 'docker-compose build --no-cache'
            }
        }

        stage('Deploy') {
            steps {
                sh 'docker-compose up -d --force-recreate'
            }
        }

        stage('Verify') {
            steps {
                sh 'docker ps'
            }
        }
    }
}
