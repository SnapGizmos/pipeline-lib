package com.snapgizmos.cicd

/**
 * Created by tite on 12/25/16.
 */
class OpenshiftCLI implements Serializable {
    private Script script = null;
    private Map config = null;
//    private Map config = new HashMap();

    def OpenshiftCLI(Script script, Map config) {
        script.sh "echo TITE: OpenshiftCLI constructur!!"
        this.script = script
        this.config = config
        script.echo "TITE: OpenshiftCLI constructur!! /end "
    }

    public String toString() {
//        return "[Person: firstName=" + firstName +
//            " lastName=" + lastName +
//            " age=" + age +
//            " spouse=" + spouse.getFirstName() +
//            "]";
        return this.getClass().getName() + "@" + Integer.toHexString(this.hashCode());
    }
}
