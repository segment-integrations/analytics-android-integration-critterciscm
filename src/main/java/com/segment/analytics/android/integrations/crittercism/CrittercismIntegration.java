package com.segment.analytics.android.integrations.crittercism;

import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.analytics.Analytics;
import com.segment.analytics.ValueMap;
import com.segment.analytics.integrations.IdentifyPayload;
import com.segment.analytics.integrations.Integration;
import com.segment.analytics.integrations.ScreenPayload;
import com.segment.analytics.integrations.TrackPayload;

import static com.segment.analytics.internal.Utils.isNullOrEmpty;

/**
 * Crittercism is an error reporting tool for your mobile apps. Any time your app crashes or
 * errors.
 * Crittercism will collect logs that will help you debug the problem and fix your app.
 *
 * @see <a href="http://crittercism.com">Crittercism</a>
 * @see <a href="https://segment.com/docs/integrations/crittercism">Crittercism Integration</a>
 * @see <a href="http://docs.crittercism.com/android/android.html">Crittercism Android SDK</a>
 */
public class CrittercismIntegration extends Integration<Void> {
  public static final Factory FACTORY = new Factory() {
    @Override public Integration<?> create(ValueMap settings, Analytics analytics) {
      return new CrittercismIntegration(analytics, settings);
    }

    @Override public String key() {
      return CRITTERCISM_KEY;
    }
  };
  private static final String CRITTERCISM_KEY = "Crittercism";

  CrittercismIntegration(Analytics analytics, ValueMap settings) {
    CrittercismConfig config = new CrittercismConfig();

    boolean shouldCollectLogcat = settings.getBoolean("shouldCollectLogcat", false);
    config.setLogcatReportingEnabled(shouldCollectLogcat);

    boolean includeVersionCode = settings.getBoolean("includeVersionCode", false);
    config.setVersionCodeToBeIncludedInVersionString(includeVersionCode);

    String customVersionName = settings.getString("customVersionName");
    if (!isNullOrEmpty(customVersionName)) {
      config.setCustomVersionName(customVersionName);
    }

    boolean enableServiceMonitoring = settings.getBoolean("enableServiceMonitoring", true);
    config.setServiceMonitoringEnabled(enableServiceMonitoring);

    Crittercism.initialize(analytics.getApplication(), settings.getString("appId"), config);
  }

  @Override public Void getUnderlyingInstance() {
    return null;
  }

  @Override public void identify(IdentifyPayload identify) {
    super.identify(identify);
    Crittercism.setUsername(identify.userId());
    Crittercism.setMetadata(identify.traits().toJsonObject());
  }

  @Override public void screen(ScreenPayload screen) {
    super.screen(screen);
    Crittercism.leaveBreadcrumb(String.format("Viewed %s Screen", screen.event()));
  }

  @Override public void track(TrackPayload track) {
    super.track(track);
    Crittercism.leaveBreadcrumb(track.event());
  }

  @Override public void flush() {
    super.flush();
    Crittercism.sendAppLoadData();
  }
}
