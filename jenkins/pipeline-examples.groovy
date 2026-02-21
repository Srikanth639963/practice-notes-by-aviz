#Declaratieve Pipeline

pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
    }
}

---

#multistage pipeline

pipeline {
    agent any
    stages {
        stage('Build') {
            steps {
                echo 'Building...'
            }
        }
        stage('Test') {
            steps {
                echo 'Testing...'
            }
        }
        stage('Deploy') {
            steps {
                echo 'Deploying...'
            }
        }
    }
}

---

pipeline {
    agent any
    stages {
        stage('shell-test') {
            steps {
                sh 'uname'
            }
        }
        stage('shell-test2') {
            steps {
                sh 'df -Th'
            }
        }
    }
}

---

pipeline {
    agent any
    stages {
        stage('checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/avizway1/awar06-jenkins.git'
            }
        }
    }
}

---

pipeline {
    agent any
    tools {
        maven "maven"
    }
    stages {
        stage('checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/avizway1/awar06-jenkins.git'
            }
        }
        stage('maven-build') {
            steps {
                sh 'echo "We are building code now"'
                sh "mvn -Dmaven.test.failure.ignore=true clean package"
            }
        }
    }
}

---

pipeline {
    agent any
    tools {
        maven "maven"
    }
    stages {
        stage('checkout') {
            steps {
                git branch: 'main', url: 'https://github.com/avizway1/awar06-jenkins.git'
            }
        }
        stage('maven-build') {
            steps {
                sh 'echo "We are building code now"'
                sh "mvn -Dmaven.test.failure.ignore=true clean package"
            }
        }
        stage('deploy-to-tomcat') {
            steps {
                deploy adapters: [
                    tomcat9(
                        credentialsId: 'tomcat',
                        path: '',
                        url: 'http://172.31.38.49:8080'
                    )
                ],
                contextPath: null,
                war: '**/*.war'
            }
        }
    }
}

---
## Add as environment variables

pipeline {
    agent any
    tools {
        maven "maven"
    }
    environment {
        GIT_REPO_URL   = 'https://github.com/avizway1/awar06-jenkins.git'
        GIT_BRANCH     = 'main'
        TOMCAT_IP      = '172.31.38.49'
        TOMCAT_PORT    = '8080'
        TOMCAT_CRED_ID = 'tomcat'
    }
    stages {
        stage('checkout') {
            steps {
                git branch: "${GIT_BRANCH}", url: "${GIT_REPO_URL}"
            }
        }
        stage('maven-build') {
            steps {
                sh 'echo "We are building code now"'
                sh "mvn -Dmaven.test.failure.ignore=true clean package"
            }
        }
        stage('deploy-to-tomcat') {
            steps {
                deploy adapters: [
                    tomcat9(
                        credentialsId: "${TOMCAT_CRED_ID}",
                        path: '',
                        url: "http://${TOMCAT_IP}:${TOMCAT_PORT}"
                    )
                ],
                contextPath: null,
                war: '**/*.war'
            }
        }
    }
}

---

#Build with parameters

pipeline {
    agent any
    tools {
        maven "maven"
    }
    parameters {
        string(name: 'GIT_REPO_URL',   defaultValue: 'https://github.com/avizway1/awar06-jenkins.git', description: 'Enter Git Repository URL')
        string(name: 'GIT_BRANCH',     defaultValue: 'main',           description: 'Git Branch to build')
        string(name: 'TOMCAT_IP',      defaultValue: '172.31.38.49',   description: 'Tomcat Server IP Address')
        string(name: 'TOMCAT_PORT',    defaultValue: '8080',           description: 'Tomcat Server Port')
        string(name: 'TOMCAT_CRED_ID', defaultValue: 'tomcat',          description: 'Jenkins Credential ID for Tomcat')
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
}


----

## get notification to slack based on build failures or success

pipeline {
    agent any
    tools {
        maven "maven"
    }
    parameters {
        string(name: 'GIT_REPO_URL',   defaultValue: 'https://github.com/avizway1/awar06-jenkins.git', description: 'Git Repository URL')
        string(name: 'GIT_BRANCH',     defaultValue: 'main',           description: 'Git Branch to build')
        string(name: 'TOMCAT_IP',      defaultValue: '172.31.30.24',   description: 'Tomcat Server IP Address')
        string(name: 'TOMCAT_PORT',    defaultValue: '8080',           description: 'Tomcat Server Port')
        string(name: 'TOMCAT_CRED_ID', defaultValue: 'tomcat',          description: 'Jenkins Credential ID for Tomcat')
    }
    environment {
        SLACK_WEBHOOK_URL = credentials('slack-webhook')
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