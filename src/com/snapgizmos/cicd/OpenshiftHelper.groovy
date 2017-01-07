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

    def deleteWaitResourceByJsonYaml(String jsonyaml, String namespace = null, boolean verbose = false, int wait = 30) {
        // 1.- Parse the jsonyaml in order to fetch the list of objects
        try {

        } catch (Exception e) {

        }

        // 1.- call the openshift-pipeline-plugin with the jsonyaml in order to get the deletions going ..
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
        def strTemplate = ''
        try {
            def tmp
            script.echo "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'"
            script.echo "Raw template is ${tmplName}"
            tmp = script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'", returnStdout: true
            script.echo "Script ran, with this output: ${tmp}"
            if (tmp) {
                def strParams = this.getParams(tmp.tokenize("\n"))
                strTemplate = script.sh script: "oc process -n ${this.config.namespace} -o yaml ${tmplName} ${strParams} ", returnStdout: true
//                script.echo "strTemplate = ${strTemplate}"
            }
        } catch (Exception e) {
            script.echo 'most likely ... the template is no ther'
            script.echo e.dump()
        }

        /** **
         2.- parse template file so we can get the objects within. The idea here is to be able to
         delete them from the openshift cluster, so objects get refreshed when reprocessing the template
         /** **/
        script.echo "OpenshiftHelper.processTemplate($tname) 2.- parse old processed template so we can get the objects for deletion related to the template "
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
//            script.echo e.dump()
        }
        script.echo 'I believe we are done with deletion 1... '
        /** **/
        def tmplItems = new HashMap()
        try {
            def yamlParser
            def ymlTemplate = new Yaml()
            yamlParser = ymlTemplate.load(strTemplate.toString())
            def aObj = yamlParser.get('items')
            def j = aObj.size()
            for (int i = 0; i < j; i++) {
                def itm = aObj[i]
                try {
                    if (!tmplItems[itm['kind']]) tmplItems[itm['kind']] = []
                    tmplItems[itm['kind']].add(itm)
                    script.echo "Iterating over ${itm} "
//                    script.sh "oc delete ${itm['kind']}/${itm['metadata']['name']} -n ${this.config.namespace} "
//                    script.openshiftDeleteResourceByKey types: itm['kind'].toString().toLowerCase(), keys: itm['metadata']['name'], namespace: this.config.namespace, verbose: 'false'
//                    def tmp = script.sh script: "oc get ${itm['kind']}/${itm['metadata']['name']} -n ${this.config.namespace} ", returnStdout: 'true'
                    script.echo "Status OK "
//                    if (tmp) {
//                        script.echo "DID NOT delete ${itm['kind']/${itm['metadata']['name']}}"
//                    } else {
//                        script.echo "SUCCESSFULL delete ${itm['kind']/${itm['metadata']['name']}}"
//                    }
                } catch (Exception e) {
                    script.echo "Did _NOT_ delete entry ${itm['kind']}/${itm['metadata']['name']}"
//                    script.sh "oc get ${itm['kind']}/${itm['metadata']['name']} -n ${this.config.namespace} 2>/dev/stdout || echo 'sh failed' "
//                    script.echo e.dump()
                }
            }
            println('phony')
        } catch (Exception e) {
            script.echo "Did not _delete_ template contents .. in general"
            script.echo e.dump()
        }
        try {
            script.echo "validations are ${tmplItems.size()}"
            def keys = tmplItems.keySet() as String[]
            for (def i = 0; i < keys.size(); i++) {
                try {
                    def key = keys[i]
//                    script.echo "key: ${key} : size: ${tmplItems[key].size()}"
                    for (def j = 0; j < tmplItems[key].size(); j++) {
                        script.echo "key: ${key} Hash : ${tmplItems[key][j]} "
                        try {
                            for (def k = 0; k < 20; k++) {
                                script.echo "Testing ${key}/${tmplItems[key][j]['metadata']['name']} .. for deleteion # ${k}"
                                script.sh "oc get ${key}/${tmplItems[key][j]['metadata']['name']} -n ${this.config.namespace} "
                                sleep(30000)
                            }
                        } catch (Exception e) {
                            script.echo "Successfully deleted ${key}/${tmplItems[key][j]['metadata']['name']} . "
                        }
                    }
                } catch (Exception e) {
                    script.echo "DOES NOT EXIST "
                    script.echo e.dump()
                }
            }
        } catch (Exception e) {
            script.echo "Did not _VALIDATE template contents .. in general"
            script.echo e.dump()
        }
        /** **/

        /** **
         3.- create this new template we have on file
         /** **/
        try {
            script.echo "OpenshiftHelper.processTemplate($tname) 3.- create this new template we have on file "
            script.echo "------------------- This is the Actual TeMPLate ----------------"
//            script.echo strFile
            script.openshiftCreateResource jsonyaml: strFile, namespace: config.namespace, verbose: 'false'
            script.echo 'status OK'
        } catch (Exception e) {
            script.echo "While creatingResource - Silengly ignoring exception : "
//        script.echo e.getStackTrace()
            script.echo e.dump()
        }

        /** **
         4.- compile the parameters from the configuration environment that this template asks for within the parameters
         /** **/
        try {
            script.echo "OpenshiftHelper.processTemplate($tname) 4.- compile the parameters from the configuration environment that this template asks for within the parameters"
//            script.echo "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$'"
            script.echo "describe template/${tmplName} "
            script.sh "oc describe templates/${tmplName} -n ${config.nameserver} 2>/dev/stdout || echo 'sh failed' "
//            script.echo "process template/${tmplName} "
//            script.sh "oc process --parameters -n ${this.config.namespace} ${tmplName} 2>/dev/stdout || echo 'sh failed' "
            script.echo "Raw template is ${tmplName}"
            def tmp = script.sh script: "oc process --parameters -n ${this.config.namespace} ${tmplName} | grep -oh '^\\w*' | grep -v '^NAME\$' || echo 'sh failed'", returnStdout: true
            script.echo "Template PARAMS: \n${tmp}"
            if (tmp) {
                def strParams = this.getParams(tmp.tokenize("\n"))
                script.echo "rendering template with these params: ${strParams}"
                strTemplate = script.sh script: "oc process -n ${this.config.namespace} -o yaml ${tmplName} ${strParams} ", returnStdout: true
                script.echo "FINAL TEMPLATE ${strTemplate} "
            }
        } catch (Exception e) {
            script.echo e.dump()
        }

        /** **
         5.- render the template with all of the matching parameters, so objects are created
         /** **/
        try {
            script.echo "OpenshiftHelper.processTemplate($tname) 5.- render the template with all of the matching parameters, so objects are created"
            if (strTemplate) {
//                script.echo strTemplate
                script.openshiftCreateResource jsonyaml: strTemplate, namespace: ${this.config.namespace}, verbose: 'false'
            }
        } catch (Exception e) {
            script.echo "Silengly ignoring exception : "
//        script.echo e.getStackTrace()
            script.echo e.dump()
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
            if (itm) params = "${params}${key}=\'${itm}\' "
        }
        this.script.sh "echo params ${params} "
        return params
    }

}
