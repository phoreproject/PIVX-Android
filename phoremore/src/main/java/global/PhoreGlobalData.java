package global;

import java.util.ArrayList;
import java.util.List;

import phoremore.PhorePeerData;

/**
 * Created by furszy on 7/2/17.
 */

public class PhoreGlobalData {

    public static final String[] TRUSTED_NODES = new String[] {
        "10.0.3.2",
        // "45.77.140.43",
        // "217.182.83.168",
        // "207.246.120.133",
        // "207.148.26.232",
    };

    public static final List<PhorePeerData> listTrustedHosts() {
        List<PhorePeerData> list = new ArrayList<>();
        for (String trustedNode : TRUSTED_NODES) {
            list.add(new PhorePeerData(trustedNode,11774,11774));
        }
        return list;
    }

}
