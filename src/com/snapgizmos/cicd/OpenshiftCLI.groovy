package com.snapgizmos.cicd

@Grapes(
        @Grab(group='org.json', module='json', version='20160810')
)
import org.json.JSONObject;

/**
 * Created by tite on 12/25/16.
 */
class OpenshiftCLI implements Serializable {
    private Script script = null;
    private Map config = null;
//    private Map config = new HashMap();

//    @NonCPS
    def OpenshiftCLI(Script script, Map config) {
        println "echo TITE: OpenshiftCLI constructur!!"
        this.script = script
        this.config = config
        println "TITE: OpenshiftCLI constructur!! /end "
    }

    def renderTemplate() {
        script.echo "OpenshiftCLI.renderTemplate() Config is actually '${config}' "
        def strFile = script.readFile file: "openshift/templates/test.json"
        def jsonArray = JSONObject( strFile )
        script.echo jsonArray
    }

}
