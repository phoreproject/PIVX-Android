package io.phore.android.module;

import android.content.SharedPreferences;

import org.phorej.core.Context;
import org.phorej.core.NetworkParameters;

import global.WalletConfiguration;
import io.phore.android.utils.Configurations;

import static io.phore.android.module.PhoreContext.CONTEXT;
import static io.phore.android.module.PhoreContext.Files.BLOCKCHAIN_FILENAME;
import static io.phore.android.module.PhoreContext.Files.CHECKPOINTS_FILENAME;
import static io.phore.android.module.PhoreContext.Files.WALLET_FILENAME_PROTOBUF;
import static io.phore.android.module.PhoreContext.Files.WALLET_KEY_BACKUP_PROTOBUF;
import static io.phore.android.module.PhoreContext.NETWORK_PARAMETERS;
import static io.phore.android.module.PhoreContext.PEER_DISCOVERY_TIMEOUT_MS;
import static io.phore.android.module.PhoreContext.PEER_TIMEOUT_MS;

/**
 * Created by furszy on 6/4/17.
 */

import android.util.Log;

public class WalletConfImp extends Configurations implements WalletConfiguration {

    private static final String PREF_TRUSTED_NODE = "trusted_node";
    private static final String PREFS_KEY_SCHEDULE_BLOCKCHAIN_SERVICE = "sch_block_serv";

    public WalletConfImp(SharedPreferences prefs) {
        super(prefs);
    }

    @Override
    public String getTrustedNodeHost() {
        return getString(PREF_TRUSTED_NODE,null);
    }

    @Override
    public void saveTrustedNode(String host, int port) {
        save(PREF_TRUSTED_NODE,host);
    }

    @Override
    public void saveScheduleBlockchainService(long time){
        save(PREFS_KEY_SCHEDULE_BLOCKCHAIN_SERVICE,time);
    }

    @Override
    public long getScheduledBLockchainService(){
        return getLong(PREFS_KEY_SCHEDULE_BLOCKCHAIN_SERVICE,0);
    }

    @Override
    public int getTrustedNodePort() {
        return PhoreContext.NETWORK_PARAMETERS.getPort();
    }

    @Override
    public String getMnemonicFilename() {
        return PhoreContext.Files.BIP39_WORDLIST_FILENAME;
    }

    @Override
    public String getWalletProtobufFilename() {
        return WALLET_FILENAME_PROTOBUF;
    }

    @Override
    public NetworkParameters getNetworkParams() {
        return PhoreContext.NETWORK_PARAMETERS;
    }

    @Override
    public String getKeyBackupProtobuf() {
        return WALLET_KEY_BACKUP_PROTOBUF;
    }

    @Override
    public long getWalletAutosaveDelayMs() {
        return PhoreContext.Files.WALLET_AUTOSAVE_DELAY_MS;
    }

    @Override
    public Context getWalletContext() {
        return CONTEXT;
    }

    @Override
    public String getBlockchainFilename() {
        return BLOCKCHAIN_FILENAME;
    }

    @Override
    public String getCheckpointFilename() {
        return CHECKPOINTS_FILENAME;
    }

    @Override
    public int getPeerTimeoutMs() {
        return PEER_TIMEOUT_MS;
    }

    @Override
    public long getPeerDiscoveryTimeoutMs() {
        return PEER_DISCOVERY_TIMEOUT_MS;
    }

    @Override
    public int getMinMemoryNeeded() {
        return PhoreContext.MEMORY_CLASS_LOWEND;
    }

    @Override
    public long getBackupMaxChars() {
        return PhoreContext.BACKUP_MAX_CHARS;
    }

    @Override
    public boolean isTest() {
        return PhoreContext.IS_TEST;
    }

    @Override
    public int getProtocolVersion() {
        return NETWORK_PARAMETERS.getProtocolVersionNum(NetworkParameters.ProtocolVersion.CURRENT);
    }

}
