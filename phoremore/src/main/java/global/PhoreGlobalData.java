package global;

import java.util.ArrayList;
import java.util.List;

import phoremore.PhorePeerData;

/**
 * Created by furszy on 7/2/17.
 */

public class PhoreGlobalData {

    public static final String[] TRUSTED_NODES = new String[] {
        "phore.timhorton.ninja",
    };

    public static final List<PhorePeerData> listTrustedHosts() {
        List<PhorePeerData> list = new ArrayList<>();
        for (String trustedNode : TRUSTED_NODES) {
            list.add(new PhorePeerData(trustedNode,11774,11774));
        }
        return list;
    }

}
