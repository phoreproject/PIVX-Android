package phoremore.listeners;

import java.util.List;

import phoremore.PhorePeer;
import phoremore.messages.responses.StatusHistory;
import phoremore.messages.responses.Unspent;
import phoremore.utility.TxHashHeightWrapper;

/**
 * Created by furszy on 6/17/17.
 */

public interface PeerDataListener {

    void onSubscribedAddressChange(PhorePeer phoremorePeer, String address, String status);

    void onListUnpent(PhorePeer phoremorePeer,String address, List<Unspent> unspent);

    void onBalanceReceive(PhorePeer phoremorePeer, String address, long confirmed, long unconfirmed);

    void onGetHistory(PhorePeer phoremorePeer, StatusHistory statusHistory);
}
