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
//    script.echo strFile
//    def is = new ByteArrayInputStream(strFile.getBytes(StandardCharsets.UTF_8))
//    def is = new File(baseDir,'openshift/templates/config-server-javase.yaml').newInputStream()
        script.sh "echo TITE1 "
        println "TITE1 "

        def strParams = this.renderParams()

        try {
            Yaml templateYml = new Yaml()
            script.echo "TITE2 what a bitch! "
            def yamlParser = templateYml.load(strFile)
            script.echo "TITE3 holly moses! "
            def j = yamlParser.get('objects').size()
            script.echo "template is ${yamlParser.getClass()}"
            for (int i = 0; i < j; i++) {
                script.echo "Iterating ... "
                script.echo "Iterating over ${yamlParser['objects'][i]} "
//            script.sh "echo oc delete ${itm['kind']}/${itm['metadata']['name']} -n poclab "
            }
        } catch (Exception e) {
            script.echo "Silengly ignoring _expected_ exception .. "
            script.echo e.toString()
            script.echo e.getMessage()
        }

        try {
            script.openshiftCreateResource jsonyaml: strFile, namespace: 'dev', verbose: 'false'
        } catch (Exception e) {
            script.echo "Silengly ignoring exception : "
//        script.echo e.getStackTrace()
            script.echo e.toString()
        }

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
            println "going over ${key}=${itm} "
            params = "${params}${key}=\'${itm}\'\n"
        }
        this.script.sh "echo params ${params} "
        return params
    }

}
