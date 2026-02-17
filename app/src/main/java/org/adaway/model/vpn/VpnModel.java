package org.adaway.model.vpn;

import static org.adaway.model.adblocking.AdBlockMethod.VPN;
import static org.adaway.model.error.HostError.ENABLE_VPN_FAIL;

import android.content.Context;
import android.util.LruCache;

import org.adaway.R;
import org.adaway.db.AppDatabase;
import org.adaway.db.dao.HostEntryDao;
import org.adaway.db.entity.HostEntry;
import org.adaway.helper.PreferenceHelper;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.error.HostErrorException;
import org.adaway.vpn.VpnServiceControls;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import timber.log.Timber;

/**
 * This class is the model to represent VPN service configuration.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class VpnModel extends AdBlockModel {
    private final HostEntryDao hostEntryDao;
    private final LruCache<String, HostEntry> blockCache;
    private static final Set<String> logs = Collections.synchronizedSet(new LinkedHashSet<>());
    private volatile boolean recordingLogs;
    private int requestCount;

    /**
     * Constructor.
     *
     * @param context The application context.
     */
    public VpnModel(Context context) {
        super(context);
        AppDatabase database = AppDatabase.getInstance(context);
        this.hostEntryDao = database.hostEntryDao();
        this.blockCache = new LruCache<String, HostEntry>(4 * 1024) {
            @Override
            protected HostEntry create(String key) {
                // Try exact match first
                HostEntry entry = VpnModel.this.hostEntryDao.getEntry(key);
                if (entry != null) {
                    return entry;
                }
                // Walk up domain hierarchy for wildcard matches
                int dotIndex = key.indexOf('.');
                while (dotIndex != -1) {
                    String wildcardHost = "*" + key.substring(dotIndex);
                    entry = VpnModel.this.hostEntryDao.getEntry(wildcardHost);
                    if (entry != null) {
                        return entry;
                    }
                    dotIndex = key.indexOf('.', dotIndex + 1);
                }
                return null;
            }
        };
        this.recordingLogs = PreferenceHelper.getVpnDnsLogEnabled(context);
        this.requestCount = 0;
        this.applied.postValue(VpnServiceControls.isRunning(context));
    }

    @Override
    public AdBlockMethod getMethod() {
        return VPN;
    }

    @Override
    public void apply() throws HostErrorException {
        // Clear cache
        this.blockCache.evictAll();
        // Start VPN
        boolean started = VpnServiceControls.start(this.context);
        this.applied.postValue(started);
        if (!started) {
            throw new HostErrorException(ENABLE_VPN_FAIL);
        }
        setState(R.string.status_vpn_configuration_updated);
    }

    @Override
    public void revert() {
        VpnServiceControls.stop(this.context);
        this.applied.postValue(false);
    }

    @Override
    public boolean isRecordingLogs() {
        return this.recordingLogs;
    }

    @Override
    public void setRecordingLogs(boolean recording) {
        Timber.d("VpnModel[%s] setRecordingLogs: %s", System.identityHashCode(this), recording);
        this.recordingLogs = recording;
        PreferenceHelper.setVpnDnsLogEnabled(this.context, recording);
    }

    @Override
    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    @Override
    public void clearLogs() {
        logs.clear();
    }

    /**
     * Checks host entry related to an host name.
     *
     * @param host A hostname to check.
     * @return The related host entry.
     */
    public HostEntry getEntry(String host) {
        // Compute miss rate periodically
        this.requestCount++;
        if (this.requestCount >= 1000) {
            int hits = this.blockCache.hitCount();
            int misses = this.blockCache.missCount();
            double missRate = 100D * (hits + misses) / misses;
            Timber.d("Host cache miss rate: %s.", missRate);
            this.requestCount = 0;
        }
        // Add host to logs
        if (this.recordingLogs) {
            Timber.d("VpnModel[%s] Adding host to logs: %s", System.identityHashCode(this), host);
            logs.add(host);
        } else {
             Timber.d("VpnModel[%s] NOT logging host (recording=false): %s", System.identityHashCode(this), host);
        }
        // Check cache
        return this.blockCache.get(host);
    }

    /**
     * Clears a specific host from the block cache.
     *
     * @param host The host to remove from the cache.
     */
    public void clearBlockCache(String host) {
        this.blockCache.remove(host);
    }
}
