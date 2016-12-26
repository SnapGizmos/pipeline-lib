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
        def tmplName

        try {
            def ymlTemplate = new Yaml()
            def yamlParser
            yamlParser = ymlTemplate.load(strFile)
            tmplName = yamlParser.get('metadata').get('name').toString()
            script.echo "tmplName = ${tmplName}"
        } catch (Exception e) {
            script.echo "Silengly ignoring _expected_ exception .. "
            script.echo "toString ${e.toString()} "
            script.echo "getMessage ${e.getMessage()} "
            throw e
        }
        def strTemplate
        try {
            def tmp
            script.echo "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'"
            script.echo "Raw template is ${tmplName}"
            tmp = script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'", returnStdout: true
            script.echo "Script ran, with this output ${tmp}"
            if (tmp) {
                def strParams = this.getParams(tmp.tokenize("\n"))
                strTemplate = script.sh script: "oc process -n ${this.config.namespace} -o yaml ${tmplName} ${strParams} ", returnStdout: true
                script.echo strTemplate
            }
        } catch (Exception e) {
            script.echo e.dump()
        }

        /** **
         2.- parse template file so we can get the objects within. The idea here is to be able to
         delete them from the openshift cluster, so objects get refreshed when reprocessing the template
         /** **/
        script.echo "OpenshiftHelper.processTemplate($tname) 2.- parse old processed template file so we can get the objects for deletion related to the template "
        /** **
         def yamlParser
         def ymlTemplate = new Yaml()
         script.echo "Object created"
         yamlParser = ymlTemplate.load(strTemplate)
         //        script.echo "Object loaded ${yamlParser}"
         script.echo "aObjc is of class  ${aObj.getClass().getName()}"
         def j = aObj.size()
         script.echo "template class is ${yamlParser.getClass().getName()} "
         /** **/
        try {
            script.openshiftDeleteResourceByKey types: 'template', keys: tmplName, namespace: this.config.namespace, verbose: 'false'
            script.echo "Now that we have deleted the template ... "

            if (strTemplate) {
                script.openshiftDeleteResourceByJsonYaml jsonyaml: strTemplate, namespace: config.namespace, verbose: 'false'
            }
        } catch (Exception e) {
            script.echo "The deletion of the whole jsonyaml did not cut it ... "
            script.echo e.dump()
        }
        script.echo 'I believe we are done with deletion 1... '
        /** **/
        try {

            def yamlParser
            def ymlTemplate = new Yaml()
            yamlParser = ymlTemplate.load(strTemplate)
            def aObj = yamlParser.get('items')
            def j = aObj.size()
            for (int i = 0; i < j; i++) {
                try {
                    def itm = aObj[i]
                    script.echo "Iterating over ${itm} "
//            script.sh "echo oc delete ${itm['kind']}/${itm['metadata'].get('name')} -n ${this.config.namespace} "
                    script.openshiftDeleteResourceByKey types: itm['kind'], keys: itm['metadata']['name'], namespace: this.config.namespace, verbose: 'false'
                    script.echo "next!"
                } catch (Exception e) {
                    script.echo "Did _NOT_ delete entry ${itm['kind']} / ${itm['metadata']['name']}"
//                    script.echo e.dump()
                }
            }
        } catch (Exception e) {
            script.echo e.dump()
        }
        /** **/

        /** **
         3.- create this new template we have on file
         /** **/
        try {
            script.echo "------------------- This is the Actual TeMPLate ----------------"
            script.echo strFile
            script.openshiftCreateResource jsonyaml: strFile, namespace: config.namespace, verbose: 'true'
        } catch (Exception e) {
            script.echo "Silengly ignoring exception : "
//        script.echo e.getStackTrace()
            script.echo e.dump()
        }

        /** **
         4.- compile the parameters from the configuration environment that this template asks for within the parameters
         /** **/
        try {
            def tmp
            script.echo "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'"
            script.echo "Raw template is ${tmplName}"
            tmp = script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'", returnStdout: true
            if (tmp) {
                def strParams = this.getParams(tmp.tokenize("\n"))
                strTemplate = script.sh script: "oc process -n ${this.config.namespace} -o yaml ${tmplName} ${strParams} ", returnStdout: true
                script.echo strTemplate
            }
        } catch (Exception e) {
            script.echo e.dump()
        }

        /** **
         5.- render the template with all of the matching parameters, so objects are created
         /** **/
        try {
            if (strTemplate) {
                script.openshiftCreateResource jsonyaml: strTemplate, namespace: config.namespace, verbose: 'false'
            }
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

    def getParams(def keys) {
        this.script.echo "config environments vars #: " + this.config.environment.size()
        def params = ''
        if (!keys) {
            keys = this.config.environment.keySet() as String[]
        } else if (keys in String) {
            this.script.echo "OpenshiftHelper.getParams(${keys}) of class ${keys.getCanonicalName()}"
        }
        def j = keys.size()
        for (def i = 0; i < j; i++) {
            def key = keys[i]
            def itm = this.config.environment[key]
//            this.script.echo "going over ${key}=${itm} "
            if (itm) params = "${params}${key}=\'${itm}\'\n"
        }
        this.script.sh "echo params ${params} "
        return params
    }

}
