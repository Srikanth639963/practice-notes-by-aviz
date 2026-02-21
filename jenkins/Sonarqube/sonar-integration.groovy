pipeline {
    agent any
    tools {
        maven "maven"
    }
    parameters {
        string(name: 'GIT_REPO_URL',   defaultValue: 'https://github.com/avizway1/awar06-jenkins.git', description: 'Git Repository URL')
        string(name: 'GIT_BRANCH',     defaultValue: 'main',           description: 'Git Branch to build')
        string(name: 'TOMCAT_IP',      defaultValue: '172.31.38.49',   description: 'Tomcat Server IP Address')
        string(name: 'TOMCAT_PORT',    defaultValue: '8080',           description: 'Tomcat Server Port')
        string(name: 'TOMCAT_CRED_ID', defaultValue: 'tomcat',          description: 'Jenkins Credential ID for Tomcat')
    }
    environment {
        SLACK_WEBHOOK_URL = credentials('slack-webhook')
        SONAR_TOKEN = credentials('sonarqube')
    }
    stages {
        stage('checkout') {
            steps {
                git branch: "${params.GIT_BRANCH}", url: "${params.GIT_REPO_URL}"
            }
        }
        stage('maven-build') {
            steps {
                sh 'echo "We are building code now"'
                sh "mvn -Dmaven.test.failure.ignore=true clean package"
            }
        }
        stage('sonarqube-analysis') {
            steps {
                withSonarQubeEnv('sonarqube') {
                    sh """
                        mvn sonar:sonar \
                        -Dsonar.projectKey=portalproject \
                        -Dsonar.token=${SONAR_TOKEN}
                    """
                }
            }
        }
        stage('quality-gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }
        stage('deploy-to-tomcat') {
            steps {
                deploy adapters: [
                    tomcat9(
                        credentialsId: "${params.TOMCAT_CRED_ID}",
                        path: '',
                        url: "http://${params.TOMCAT_IP}:${params.TOMCAT_PORT}"
                    )
                ],
                contextPath: null,
                war: '**/*.war'
            }
        }
    }
    post {
        success {
            sh """
                curl -X POST -H 'Content-type: application/json' \
                --data '{"text": ":white_check_mark: *BUILD SUCCESS* \\nJob: *${JOB_NAME}* \\nBuild: *#${BUILD_NUMBER}* \\nBranch: *${params.GIT_BRANCH}* \\nURL: ${BUILD_URL}"}' \
                ${SLACK_WEBHOOK_URL}
            """
        }
        failure {
            sh """
                curl -X POST -H 'Content-type: application/json' \
                --data '{"text": ":x: *BUILD FAILED* \\nJob: *${JOB_NAME}* \\nBuild: *#${BUILD_NUMBER}* \\nBranch: *${params.GIT_BRANCH}* \\nURL: ${BUILD_URL}"}' \
                ${SLACK_WEBHOOK_URL}
            """
        }
        always {
            sh """
                curl -X POST -H 'Content-type: application/json' \
                --data '{"text": ":bell: *BUILD COMPLETED* \\nJob: *${JOB_NAME}* \\nStatus: *${currentBuild.currentResult}* \\nBuild: *#${BUILD_NUMBER}* \\nURL: ${BUILD_URL}"}' \
                ${SLACK_WEBHOOK_URL}
            """
        }
    }
}
