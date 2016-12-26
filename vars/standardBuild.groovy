#!groovy

import com.snapgizmos.cicd.OpenshiftHelper;

def origin(body) {
    def config = [:]
    def nodetype

    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

//    config.environment.each { k, v -> println "out: going over ${k}=${v} " }
    if (!config.targetStages) {
        config.targetStages = ['Build','Test and Analysis','Push to Nexus','Deploy DEV','Deploy STAGE']
    }

    if (true || 'Build' in config.targetStages) config.nodetype = 'maven'
    node(config.nodetype) {
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
                params="${params}${itm.key}=\'${itm.value}\'\n"
            }
            sh "echo params ${params} "

            writeFile file:'openshift/env', text: params
            sh "cat $WORKSPACE/openshift/env "

            try {
//                def oscli = new OpenshiftHelper('this','config');
                def oscli = new OpenshiftHelper(this,config);
//            def oscli = new OpenshiftHelper();
                println "openshift cli is : ${oscli}"
                oscli.testJson()
            } catch (Exception e) {
                println e.toString()
            }
//            testJson(config.tmplOpenshift)
//            sh "bin/render-template.sh ${config.namespace}"
            def strFile = readFile file: "openshift/templates/${config.tmplOpenshift}"
            println "files has: ${strFile.length()}"

            try {
                oscli.processTemplate(config.tmplOpenshift)
//            testJson(config.tmplOpenshift)
            } catch (Exception e) {
                println e.toString()
            }

            echo "called local testJson. Testing the bad apple"
            Yaml templateYml = new Yaml()
            def yamlParser = templateYml.load(strFile)
            echo "template is ${yamlParser.getClass()}"

        }

        stage('Deploy STAGE') {
            input message: "Promote to STAGE?", ok: "Promote"
            // tag for stage
            sh "${ocCmd} tag dev/tasks:latest stage/tasks:${version}"
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

