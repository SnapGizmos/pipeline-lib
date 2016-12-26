package com.snapgizmos.cicd

@Grapes(
        @Grab('org.yaml:snakeyaml:1.17')
)
@Grapes(
        @Grab(group = 'org.json', module = 'json', version = '20160810')
)

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml

/**
 * Created by tite on 12/25/16.
 */
class OpenshiftHelper implements Serializable {
    private Script script = null;
    private Map config = null;
//    private Map config = new HashMap();

//    @NonCPS
    def OpenshiftHelper(Script script, Map config) {
        println "echo TITE: OpenshiftHelper constructur!!"
        this.script = script
        this.config = config
        println "TITE: OpenshiftHelper constructur!! /end "
    }

    def testJson() {
        script.echo "OpenshiftHelper.testJson() Config is actually '${config}' "
        def strFile = script.readFile file: "openshift/templates/test.json"
        def jsonArray = new JSONObject(strFile)
        script.echo jsonArray.toString()
    }

    def processTemplate(def tname) {
        println "Config is actually ${this.config}"
        script.echo "Config is actually '${this.config}' "
        def strFile = script.readFile file: "openshift/templates/${this.config.tmplOpenshift}"
//    def is = new File(baseDir,'openshift/templates/config-server-javase.yaml').newInputStream()
        /** **
        1.- obtain template name from the file, so we can then query the openshift api for stuff
        /** **/
        script.echo "OpenshiftHelper.processTemplate($tname) 1.- obtain template name from the file, so we can then query the openshift api for stuff"
        def yamlParser
        def tmplName
        try {
            Yaml templateYml = new Yaml()
            yamlParser = templateYml.load(strFile)
            tmplName = yamlParser.get('metadata').get('name')
            script.echo "tmplName = ${tmplName}"

            script.echo "Raw template is ${tmplName}"
//            def rawParams = script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$')", returnStdout: true
            script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$')"
            script.echo "Raw params is ${rawParams}"
        } catch (Exception e) {
            script.echo "Silengly ignoring _expected_ exception .. "
            script.echo e.toString()
            script.echo e.getMessage()
        }

        /** **
         2.- parse template file so we can get the objects within. The idea here is to be able to
         delete them from the openshift cluster, so objects get refreshed when reprocessing the template
         /** **/
        script.echo "OpenshiftHelper.processTemplate($tname) 2.- parse template file so we can get the objects for deletion related to the template "
        def aObj = yamlParser.get('objects')
        def j = aObj.size()
        script.echo "template class is ${yamlParser.getClass()} "
        for (int i = 0; i < j; i++) {
            def itm = aObj[i]
            script.echo "Iterating over ${itm} "
            script.sh "echo oc delete ${itm['kind']}/${itm['metadata'].get('name')} -n poclab "
        }

        /** **
         3.- create this new template we have on file
         /** **/
        try {
            script.openshiftCreateResource jsonyaml: strFile, namespace: config.namespace, verbose: 'false'
        } catch (Exception e) {
            script.echo "Silengly ignoring exception : "
//        script.echo e.getStackTrace()
            script.echo e.toString()
        }

        /** **
        4.- compile the parameters from the configuration environment that this template asks for within the parameters
        /** **/
        script.echo "Raw template is ${tmplName}"
        def rawParams = script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'", returnStdout: true
        script.echo "Raw params is ${rawParams}"
        def strParams = this.renderParams()

        /** **
        5.- render the template with all of the matching parameters, so objects are created
        /** **/

        /** **
         //        def proc = "oc delete ${itm['kind']}/${itm['metadata']['name']} -n poclab ".execute()
         //        def outputStream = new StringBuffer()
         //        proc.waitForProcessOutput(outputStream,System.err)
         //        println outputStream.toString()
         //    is.close()
         /** **/
    }

    def renderParams(def keys) {
        this.script.echo "config environments vars #: " + this.config.environment.size()
        def params = ''
        if (!keys) {
            keys = this.config.environment.keySet() as String[]
        }
        def j = keys.size()
        for (def i = 0; i < j; i++) {
            def key = keys[i]
            def itm = this.config.environment[key]
            this.script.echo "going over ${key}=${itm} "
            params = "${params}${key}=\'${itm}\'\n"
        }
        this.script.sh "echo params ${params} "
        return params
    }

}
