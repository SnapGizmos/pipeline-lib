def call(body) {
    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()
    node {
        stage('Checkout') {
            checkout scm
        }
        stage('Main') {
            docker.image(config.environment).inside {
                sh config.mainScript
            }
        }
        stage('Post') {
            sh config.postScript
        }
    }
}

def origin() {
    node('maven') {
        sh "env"

        // define commands
        def ocCmd = "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://osmaster.mgt.tite.lan:8443 --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
        // def mvnCmd = "mvn -s configuration/cicd-settings.xml"
        def mvnCmd = "mvn -s settings.xml"

        new File(".").eachFile() {
            file->println file.getAbsolutePath()
        }

        stage 'Build'
        git branch: 'master', url: 'http://gogs:3000/gogs/config-server-poc.git'
        def v = version()
        sh "${mvnCmd} clean install -DskipTests=true"

        stage 'Test and Analysis'
        parallel (
                'Test': {
                    sh "${mvnCmd} test"
                    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
                }
                ,
                'Static Analysis': {
                    sh "${mvnCmd} jacoco:report sonar:sonar -Dsonar.host.url=http://sonarqube:9000 -DskipTests=true"
                }
        )

        stage 'Push to Nexus'
        //configFileProvider([configFile(fileId: '00045d94-5f1a-4647-96d2-1172f422be0a', targetLocation: 'settings.xml', variable: 'NEXUS_SETTINGS')]) {
        //// some block
        //sh "${mvnCmd} -s $NEXUS_SETTINGS deploy -DskipTests=true"
        //}

        stage 'Deploy DEV'
        sh "alias oc=${ocCmd}"
        sh "bin/render-template.sh dev"
        sh "alias oc=oc"
        sh "rm -rf oc-build && mkdir -p oc-build/deployments"
        sh "cp target/openshift-tasks.war oc-build/deployments/ROOT.war"
        // clean up. keep the image stream
        sh "${ocCmd} delete bc,dc,svc,route -l app=tasks -n dev"
        // create build. override the exit code since it complains about exising imagestream
        sh "${ocCmd} new-build --name=tasks --image-stream=jboss-eap70-openshift --binary=true --labels=app=tasks -n dev || true"
        // build image
        sh "${ocCmd} start-build tasks --from-dir=oc-build --wait=true -n dev"
        // deploy image
        sh "${ocCmd} new-app tasks:latest -n dev"
        sh "${ocCmd} expose svc/tasks -n dev"

        stage 'Deploy STAGE'
        input message: "Promote to STAGE?", ok: "Promote"
        // tag for stage
        sh "${ocCmd} tag dev/tasks:latest stage/tasks:${v}"
        // clean up. keep the imagestream
        sh "${ocCmd} delete bc,dc,svc,route -l app=tasks -n stage"
        // deploy stage image
        sh "${ocCmd} new-app tasks:${v} -n stage"
        sh "${ocCmd} expose svc/tasks -n stage"
    }
}

def version() {
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    matcher ? matcher[0][1 as String] : null
}
