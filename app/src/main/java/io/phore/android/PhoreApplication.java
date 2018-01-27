package io.phore.android;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.github.anrwatchdog.ANRWatchDog;
import com.snappydb.SnappydbException;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.phorej.store.BlockStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import global.ContextWrapper;
import global.WalletConfiguration;
import global.utils.Io;
import phoremore.NetworkConf;
import phoremore.PhorePeerData;
import io.phore.android.contacts.ContactsStore;
import io.phore.android.module.PhoreContext;
import io.phore.android.module.PhoreModule;
import io.phore.android.module.PhoreModuleImp;
import io.phore.android.module.WalletConfImp;
import io.phore.android.module.store.SnappyBlockchainStore;
import io.phore.android.rate.db.RateDb;
import io.phore.android.service.PhoreWalletService;
import io.phore.android.utils.AppConf;
import io.phore.android.utils.CentralFormats;
import io.phore.android.utils.CrashReporter;
import store.AddressStore;

import static io.phore.android.service.IntentsConstants.ACTION_RESET_BLOCKCHAIN;
import static io.phore.android.utils.AndroidUtils.shareText;

/**
 * Created by mati on 18/04/17.
 */
@ReportsCrashes(
        mailTo = PhoreContext.REPORT_EMAIL, // my email here
        mode = ReportingInteractionMode.TOAST,
        resToastText = R.string.crash_toast_text)
public class PhoreApplication extends Application implements ContextWrapper {

    private static Logger log;

    /** Singleton */
    private static PhoreApplication instance;
    public static final long TIME_CREATE_APPLICATION = System.currentTimeMillis();
    private long lastTimeRequestBackup;

    private PhoreModule phoreModule;
    private AppConf appConf;
    private NetworkConf networkConf;

    private CentralFormats centralFormats;

    private ActivityManager activityManager;
    private PackageInfo info;

    public static PhoreApplication getInstance() {
        return instance;
    }

    private CrashReporter.CrashListener crashListener = new CrashReporter.CrashListener() {
        @Override
        public void onCrashOcurred(Thread thread, Throwable throwable) {
            log.error("crash occured..");
            throwable.printStackTrace();
            String authorities = "io.phore.android.myfileprovider";
            final File cacheDir = getCacheDir();
            // show error report dialog to send the crash
            final ArrayList<Uri> attachments = new ArrayList<Uri>();
            try {
                final File logDir = getDir("log", Context.MODE_PRIVATE);

                for (final File logFile : logDir.listFiles()) {
                    final String logFileName = logFile.getName();
                    final File file;
                    if (logFileName.endsWith(".log.gz"))
                        file = File.createTempFile(logFileName.substring(0, logFileName.length() - 6), ".log.gz", cacheDir);
                    else if (logFileName.endsWith(".log"))
                        file = File.createTempFile(logFileName.substring(0, logFileName.length() - 3), ".log", cacheDir);
                    else
                        continue;

                    final InputStream is = new FileInputStream(logFile);
                    final OutputStream os = new FileOutputStream(file);

                    Io.copy(is, os);

                    os.close();
                    is.close();

                    attachments.add(FileProvider.getUriForFile(getApplicationContext(), authorities, file));
                }
            } catch (final IOException x) {
                log.info("problem writing attachment", x);
            }
            shareText(PhoreApplication.this,"Phore wallet crash", "Unexpected crash", attachments);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        try {
            initLogging();
            log = LoggerFactory.getLogger(PhoreApplication.class);
            PackageManager manager = getPackageManager();
            info = manager.getPackageInfo(this.getPackageName(), 0);
            activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

            //Bugsee.launch(this, "9b3473f1-984c-4f70-9aef-b0cf485839fd");

            // The following line triggers the initialization of ACRA
            ACRA.init(this);
            //if (BuildConfig.DEBUG)
            //    new ANRWatchDog().start();
            CrashReporter.init(getCacheDir());
            CrashReporter.setCrashListener(crashListener);
            // Default network conf for localhost test
            networkConf = new NetworkConf();
            appConf = new AppConf(getSharedPreferences(AppConf.PREFERENCE_NAME, MODE_PRIVATE));
            centralFormats = new CentralFormats(appConf);
            WalletConfiguration walletConfiguration = new WalletConfImp(getSharedPreferences("phore_wallet",MODE_PRIVATE));
            //todo: add this on the initial wizard..
            //walletConfiguration.saveTrustedNode(HardcodedConstants.TESTNET_HOST,0);
            //AddressStore addressStore = new SnappyStore(getDirPrivateMode("address_store").getAbsolutePath());
            ContactsStore contactsStore = new ContactsStore(this);
            phoreModule = new PhoreModuleImp(this, walletConfiguration,contactsStore,new RateDb(this));
            phoreModule.start();

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public void startPhoreService() {
        Intent intent = new Intent(this,PhoreWalletService.class);
        startService(intent);
    }

    private void initLogging() {
        final File logDir = getDir("log", MODE_PRIVATE);
        final File logFile = new File(logDir, "app.log");
        final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        final PatternLayoutEncoder filePattern = new PatternLayoutEncoder();
        filePattern.setContext(context);
        filePattern.setPattern("%d{HH:mm:ss,UTC} [%thread] %logger{0} - %msg%n");
        filePattern.start();

        final RollingFileAppender<ILoggingEvent > fileAppender = new RollingFileAppender<ILoggingEvent>();
        fileAppender.setContext(context);
        fileAppender.setFile(logFile.getAbsolutePath());

        final TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<ILoggingEvent>();
        rollingPolicy.setContext(context);
        rollingPolicy.setParent(fileAppender);
        rollingPolicy.setFileNamePattern(logDir.getAbsolutePath() + "/wallet.%d{yyyy-MM-dd,UTC}.log.gz");
        rollingPolicy.setMaxHistory(7);
        rollingPolicy.start();

        fileAppender.setEncoder(filePattern);
        fileAppender.setRollingPolicy(rollingPolicy);
        fileAppender.start();

        final PatternLayoutEncoder logcatTagPattern = new PatternLayoutEncoder();
        logcatTagPattern.setContext(context);
        logcatTagPattern.setPattern("%logger{0}");
        logcatTagPattern.start();

        final PatternLayoutEncoder logcatPattern = new PatternLayoutEncoder();
        logcatPattern.setContext(context);
        logcatPattern.setPattern("[%thread] %msg%n");
        logcatPattern.start();

        final LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(context);
        logcatAppender.setTagEncoder(logcatTagPattern);
        logcatAppender.setEncoder(logcatPattern);
        logcatAppender.start();

        final ch.qos.logback.classic.Logger log = context.getLogger(Logger.ROOT_LOGGER_NAME);
        log.addAppender(fileAppender);
        log.addAppender(logcatAppender);
        log.setLevel(Level.INFO);
    }

    public PhoreModule getModule(){
        return phoreModule;
    }

    public AppConf getAppConf(){
        return appConf;
    }

    @Override
    public FileOutputStream openFileOutputPrivateMode(String name) throws FileNotFoundException {
        return openFileOutput(name,MODE_PRIVATE);
    }

    @Override
    public File getDirPrivateMode(String name) {
        return getDir(name,MODE_PRIVATE);
    }

    @Override
    public InputStream openAssestsStream(String name) throws IOException {
        return getAssets().open(name);
    }

    @Override
    public boolean isMemoryLow() {
        final int memoryClass = activityManager.getMemoryClass();
        return memoryClass<=phoreModule.getConf().getMinMemoryNeeded();
    }

    @Override
    public String getVersionName() {
        return info.versionName;
    }

    @Override
    public void stopBlockchain() {
        Intent intent = new Intent(this,PhoreWalletService.class);
        intent.setAction(ACTION_RESET_BLOCKCHAIN);
        startService(intent);
    }

    public NetworkConf getNetworkConf() {
        return networkConf;
    }

    /**
     *
     * @param trustedServer
     */
    public void setTrustedServer(PhorePeerData trustedServer) {
        networkConf.setTrustedServer(trustedServer);
        phoreModule.getConf().saveTrustedNode(trustedServer.getHost(),0);
        appConf.saveTrustedNode(trustedServer);
    }

    public CentralFormats getCentralFormats() {
        return centralFormats;
    }

    public PackageInfo getPackageInfo() {
        return info;
    }

    public static long getTimeCreateApplication() {
        return TIME_CREATE_APPLICATION;
    }

    public long getLastTimeRequestedBackup() {
        return lastTimeRequestBackup;
    }

    public void setLastTimeBackupRequested(long lastTimeBackupRequested) {
        this.lastTimeRequestBackup = lastTimeBackupRequested;
    }

}
