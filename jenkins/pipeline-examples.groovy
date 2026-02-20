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
