package com.snapgizmos.cicd

/**
 * Created by tite on 12/25/16.
 */
class OpenshiftCLI {
    private Script script = null;
    private Map config = null;
//    private Map config = new HashMap();

    def osUtils(script, config) {
        println "TITE: OpenshiftCLI constructur!!"
        this.script = script
        this.config = config
    }
}
