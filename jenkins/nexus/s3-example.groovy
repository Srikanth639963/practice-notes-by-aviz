pipeline {
    agent any

    tools {
        maven 'maven'
    }

    environment {
        // Tomcat details
        TOMCAT_HOST = '65.1.1.45'
        TOMCAT_PORT = '8080'
        APP_CONTEXT = '/Springdemo-0.0.1-SNAPSHOT'

        // SonarQube details
        SONAR_HOST_URL     = 'http://172.31.6.188:9000'
        SONAR_PROJECT_KEY  = 'myprodcode'
        SONAR_PROJECT_NAME = 'prod-app-code'

        // S3 "artifactory"
        S3_BUCKET = 'aviz-code-artifactory'
    }

    stages {
        stage('checkout') {
            steps {
                git branch: 'main',
                    url: 'https://github.com/avizway1/awar05-jenkins.git'
            }
        }

        stage('build') {
            steps {
                sh '''
                    echo "Building my code using Maven..."
                    mvn clean install
                '''
            }
        }

        stage('test') {
            steps {
                sh 'mvn test'
            }
        }

        stage('sonar-analysis') {
            steps {
                withCredentials([
                    string(credentialsId: 'sonar', variable: 'SONAR_TOKEN')
                ]) {
                    sh '''
                        echo "Running SonarQube analysis..."

                        mvn org.sonarsource.scanner.maven:sonar-maven-plugin:sonar \
                          -Dsonar.projectKey=$SONAR_PROJECT_KEY \
                          -Dsonar.projectName=$SONAR_PROJECT_NAME \
                          -Dsonar.host.url=$SONAR_HOST_URL \
                          -Dsonar.token=$SONAR_TOKEN
                    '''
                }
            }
        }

        stage('publish-artifact-to-s3') {
            steps {
                sh '''
                    echo "Locating WAR file to upload to S3..."
                    WAR_FILE=$(ls target/*.war | head -n 1)

                    if [ -z "$WAR_FILE" ]; then
                      echo "No WAR file found in target/"
                      exit 1
                    fi

                    WAR_NAME=$(basename "$WAR_FILE")
                    S3_KEY="prod-app-code/${BUILD_NUMBER}/${WAR_NAME}"

                    echo "Uploading $WAR_FILE to s3://$S3_BUCKET/$S3_KEY ..."
                    aws s3 cp "$WAR_FILE" "s3://$S3_BUCKET/$S3_KEY"

                    echo "Artifact uploaded to S3: s3://$S3_BUCKET/$S3_KEY"
                '''
            }
        }

        stage('deploy-to-tomcat-from-s3') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'tomcat',
                        usernameVariable: 'TOMCAT_USER',
                        passwordVariable: 'TOMCAT_PASS'
                    )
                ]) {
                    sh '''
                        echo "Getting WAR name from S3 for this build..."

                        WAR_NAME=$(aws s3 ls "s3://$S3_BUCKET/prod-app-code/${BUILD_NUMBER}/" | awk '{print $4}' | head -n 1)

                        if [ -z "$WAR_NAME" ]; then
                          echo "No WAR found in s3://$S3_BUCKET/prod-app-code/${BUILD_NUMBER}/"
                          exit 1
                        fi

                        S3_KEY="prod-app-code/${BUILD_NUMBER}/${WAR_NAME}"
                        LOCAL_WAR="/tmp/${WAR_NAME}"

                        echo "Downloading WAR from S3: s3://$S3_BUCKET/$S3_KEY -> $LOCAL_WAR"
                        aws s3 cp "s3://$S3_BUCKET/$S3_KEY" "$LOCAL_WAR"

                        echo "Deploying $LOCAL_WAR to Tomcat..."
                        curl --fail -u "$TOMCAT_USER:$TOMCAT_PASS" \
                          -T "$LOCAL_WAR" \
                          "http://$TOMCAT_HOST:$TOMCAT_PORT/manager/text/deploy?path=$APP_CONTEXT&update=true"

                        echo "Deployment triggered at: http://$TOMCAT_HOST:$TOMCAT_PORT$APP_CONTEXT"
                    '''
                }
            }
        }
    }

    post {
        success {
            withCredentials([
                string(credentialsId: 'webhook', variable: 'SLACK_WEBHOOK_URL')
            ]) {
                sh '''
                    SONAR_DASHBOARD_URL="${SONAR_HOST_URL}/dashboard?id=${SONAR_PROJECT_KEY}"

                    PAYLOAD=$(cat <<EOF
{
  "text": "✅ *Deployment SUCCESS*\\n• Job: ${JOB_NAME}\\n• Build: #${BUILD_NUMBER}\\n• App: http://${TOMCAT_HOST}:${TOMCAT_PORT}${APP_CONTEXT}\\n• SonarQube: ${SONAR_DASHBOARD_URL}\\n• Artifact: s3://${S3_BUCKET}/prod-app-code/${BUILD_NUMBER}/\\n• Channel: #webhook-test"
}
EOF
                    )

                    curl -X POST -H 'Content-type: application/json' \
                         --data "$PAYLOAD" \
                         "$SLACK_WEBHOOK_URL"
                '''
            }
        }

        failure {
            withCredentials([
                string(credentialsId: 'webhook', variable: 'SLACK_WEBHOOK_URL')
            ]) {
                sh '''
                    PAYLOAD=$(cat <<EOF
{
  "text": "❌ *Deployment FAILED*\\n• Job: ${JOB_NAME}\\n• Build: #${BUILD_NUMBER}\\n• Console: ${BUILD_URL}\\n• Channel: #webhook-test"
}
EOF
                    )

                    curl -X POST -H 'Content-type: application/json' \
                         --data "$PAYLOAD" \
                         "$SLACK_WEBHOOK_URL"
                '''
            }
        }
    }
}

