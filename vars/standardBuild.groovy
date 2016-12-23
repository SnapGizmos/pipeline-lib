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

def origin(body) {
    def config = [:]
    def nodetype

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

//    config.environment.each { k, v -> println "out: going over ${k}=${v} " }

    if ('Build' in config.targetStages) nodetype = 'maven'
    node(nodetype) {
        if (config.showEnv) {
            sh "env"
        }
        println("TITE: got targest: ${config.targetStages}")

        // define commands
        def ocCmd = "oc --token=`cat /var/run/secrets/kubernetes.io/serviceaccount/token` --server=https://osmaster.mgt.tite.lan:8443 --certificate-authority=/run/secrets/kubernetes.io/serviceaccount/ca.crt"
        // def mvnCmd = "mvn -s configuration/cicd-settings.xml"
        def mvnCmd = "mvn -s settings.xml"

        stage('Checkout') {
            println("TITE: hey ... checking out")
            // git branch: 'master', url: 'http://gogs:3000/gogs/config-server-poc.git'
            checkout scm
        }

        if ('Build' in config.targetStages)
            stage('Build') {
                def v = version()
                sh "${mvnCmd} clean install -DskipTests=true"
            }

        if ('Push to Nexus' in config.targetStages)
        stage('Push to Nexus') {
            //configFileProvider([configFile(fileId: '00045d94-5f1a-4647-96d2-1172f422be0a', targetLocation: 'settings.xml', variable: 'NEXUS_SETTINGS')]) {
            //// some block
            sh "${mvnCmd} deploy -DskipTests=true"
        }

        if ('Deploy DEV' in config.targetStages)
        stage('Deploy DEV') {
            sh "oc get pods -n ${config.namespace}"
            sh "env"
            /** **
            Maybe the ARTIFACT_URL could be rendered based on pom.xml
             nexus.h.svc.tite.lan/service/local/artifact/maven/redirect?r=snapshots\&g=${group()}\&a=${artifact()}\&v=${version()}"
             /** **/
            println "config environments are: " + config.environment.size()
            def params=''
            for (itm in config.environment) {
                println "going over ${itm.key}=${itm.value} for "+System.getenv('WORKSPACE')
//                sh "echo ${itm.key}=${itm.value} >> $WORKSPACE/openshift/env"
                params="${params}\n${itm.key}=${itm.value}"
            }
            sh "echo params ${params} "

            writeFile file:'openshift/env', text: params
            sh "cat $WORKSPACE/openshift/env "

            def tokeyValue = {
                it.collect { /$it.key="$it.value"/ } join "\n"
            }
            println "tokeyValue is \n"+tokeyValue(config.environment)

            sh "bin/render-template.sh ${config.namespace}"

            /** old crap **
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
             /** **/
        }

        stage('Deploy STAGE') {
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
}


def version() {
    def matcher = readFile('pom.xml') =~ '<version>(.+)</version>'
    matcher ? matcher[0][1] : null
}
def artifact() {
    def matcher = readFile('pom.xml') =~ '<artifactId>(.+)</artifactId>'
    matcher ? matcher[0][1] : null
}
def group() {
    def matcher = readFile('pom.xml') =~ '<groupId>(.+)</groupId>'
    matcher ? matcher[0][1] : null
}

def origin_orig() {
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

