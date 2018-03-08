package phoremore.listeners;

import phoremore.PhorePeer;

/**
 * Created by furszy on 6/17/17.
 */

public interface PeerListener {

    void onConnected(PhorePeer phoremorePeer);

    void onDisconnected(PhorePeer phoremorePeer);

    void onExceptionCaught(PhorePeer phoremorePeer, Exception e);
}
