package com.snapgizmos.cicd

//import com.cloudbees.groovy.cps.NonCPS;

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
    }

}
