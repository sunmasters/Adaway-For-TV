package org.adaway;

import android.app.Application;

import org.adaway.helper.NotificationHelper;
import org.adaway.helper.PreferenceHelper;
import org.adaway.model.adblocking.AdBlockMethod;
import org.adaway.model.adblocking.AdBlockModel;
import org.adaway.model.source.SourceModel;
import org.adaway.model.update.UpdateModel;
import org.adaway.util.log.ApplicationLog;

import timber.log.Timber;

/**
 * This class is a custom {@link Application} for AdAway app.
 *
 * @author Bruce BUJON (bruce.bujon(at)gmail(dot)com)
 */
public class AdAwayApplication extends Application {
    /**
     * The common source model for the whole application.
     */
    private SourceModel sourceModel;
    /**
     * The common ad block model for the whole application.
     */
    private AdBlockModel adBlockModel;
    /**
     * The common update model for the whole application.
     */
    private UpdateModel updateModel;

    @Override
    public void onCreate() {
        // Delegate application creation
        super.onCreate();
        // Initialize logging
        ApplicationLog.init(this);
        // Create notification channels
        NotificationHelper.createNotificationChannels(this);
        // Create models
        this.sourceModel = new SourceModel(this);
        this.updateModel = new UpdateModel(this);
    }

    /**
     * Get the source model.
     *
     * @return The common source model for the whole application.
     */
    public SourceModel getSourceModel() {
        return this.sourceModel;
    }

    /**
     * Get the ad block model.
     *
     * @return The common ad block model for the whole application.
     */
    public AdBlockModel getAdBlockModel() {
        // Check cached model
        AdBlockMethod method = PreferenceHelper.getAdBlockMethod(this);
        
        // Debug logging for split-brain analysis
        String currentHash = this.adBlockModel == null ? "null" : String.valueOf(System.identityHashCode(this.adBlockModel));
        AdBlockMethod currentMethod = this.adBlockModel == null ? null : this.adBlockModel.getMethod();
        Timber.d("AdAwayApplication: getAdBlockModel called. Current Model Hash: %s, Current Method: %s, Pref Method: %s", currentHash, currentMethod, method);

        if (this.adBlockModel == null || this.adBlockModel.getMethod() != method) {
            Timber.d("AdAwayApplication: DECISION TO REBUILD. Reason: Model is null? %s OR Method mismatch (%s != %s)", (this.adBlockModel == null), currentMethod, method);
            Timber.d("AdAwayApplication: Building new AdBlockModel for method %s", method);
            this.adBlockModel = AdBlockModel.build(this, method);
            Timber.d("AdAwayApplication: New model built: %s (Hash: %s)", this.adBlockModel.getClass().getSimpleName(), System.identityHashCode(this.adBlockModel));
        }
        Timber.v("AdAwayApplication: Returning model Hash: %s", System.identityHashCode(this.adBlockModel));
        return this.adBlockModel;
    }

    /**
     * Get the update model.
     *
     * @return Teh common update model for the whole application.
     */
    public UpdateModel getUpdateModel() {
        return this.updateModel;
    }
}
