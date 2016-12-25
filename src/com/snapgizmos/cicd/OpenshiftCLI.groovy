package com.snapgizmos.cicd

// https://mvnrepository.com/artifact/net.sf.json-lib/json-lib
@Grapes(
    @Grab(group='net.sf.json-lib', module='json-lib', version='2.4')
)
import net.sf.json.JSONArray;

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
        boolean[] boolArray = new boolean[]{true,false,true};
        JSONArray jsonArray = JSONArray.fromObject( boolArray );
        System.out.println( jsonArray );
    }

}
