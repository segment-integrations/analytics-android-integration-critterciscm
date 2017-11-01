package com.segment.analytics.android.integrations.crittercism;

import android.app.Application;
import com.crittercism.app.Crittercism;
import com.crittercism.app.CrittercismConfig;
import com.segment.analytics.Analytics;
import com.segment.analytics.Traits;
import com.segment.analytics.ValueMap;
import com.segment.analytics.core.tests.BuildConfig;
import com.segment.analytics.test.IdentifyPayloadBuilder;
import com.segment.analytics.test.ScreenPayloadBuilder;
import com.segment.analytics.test.TrackPayloadBuilder;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static com.segment.analytics.Utils.createTraits;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 18, manifest = Config.NONE)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*", "org.json.*" })
@PrepareForTest(Crittercism.class)
public class CrittercismTest {
  @Rule public PowerMockRule rule = new PowerMockRule();
  @Mock Application context;
  @Mock Analytics analytics;
  CrittercismIntegration integration;

  @Before public void setUp() {
    initMocks(this);
    PowerMockito.mockStatic(Crittercism.class);
    when(analytics.getApplication()).thenReturn(context);

    integration = new CrittercismIntegration(analytics, new ValueMap().putValue("appId", "foo"));
  }

  @Test public void initialize() {
    ValueMap settings = new ValueMap().putValue("appId", "bar")
        .putValue("shouldCollectLogcat", true)
        .putValue("includeVersionCode", true)
        .putValue("customVersionName", "qaz")
        .putValue("enableServiceMonitoring", false);
    CrittercismIntegration.FACTORY.create(settings, analytics);

    CrittercismConfig expectedConfig = new CrittercismConfig();
    expectedConfig.setLogcatReportingEnabled(true);
    expectedConfig.setVersionCodeToBeIncludedInVersionString(true);
    expectedConfig.setCustomVersionName("qaz");
    expectedConfig.setServiceMonitoringEnabled(false);
    verifyStatic();
    Crittercism.initialize(eq(context), eq("bar"), configEq(expectedConfig));
  }

  @Test public void identify() {
    Traits traits = createTraits("foo");
    integration.identify(new IdentifyPayloadBuilder().traits(traits).build());
    verifyStatic();
    Crittercism.setUsername("foo");
    verifyStatic();
    Crittercism.setMetadata(jsonEq(traits.toJsonObject()));
  }

  @Test public void screen() {
    integration.screen(new ScreenPayloadBuilder().name("foo").category("bar").build());
    verifyStatic();
    Crittercism.leaveBreadcrumb("Viewed foo Screen");
  }

  @Test public void track() {
    integration.track(new TrackPayloadBuilder().event("foo").build());
    verifyStatic();
    Crittercism.leaveBreadcrumb("foo");
  }

  @Test public void flush() {
    integration.flush();
    verifyStatic();
    Crittercism.sendAppLoadData();
  }

  public static JSONObject jsonEq(JSONObject expected) {
    return argThat(new JSONObjectMatcher(expected));
  }

  public static CrittercismConfig configEq(CrittercismConfig crittercismConfig) {
    return argThat(new CrittercismConfigMatcher(crittercismConfig));
  }

  public static class CrittercismConfigMatcher extends TypeSafeMatcher<CrittercismConfig> {

    private final CrittercismConfig expected;

    CrittercismConfigMatcher(CrittercismConfig expected) {
      this.expected = expected;
    }

    @Override protected boolean matchesSafely(CrittercismConfig item) {
      try {
        assertEquals(expected, item);
        return true;
      } catch (AssertionError e) {
        return false;
      }
    }

    @Override public void describeTo(Description description) {
      print(expected, description);
    }

    static void print(CrittercismConfig config, Description description) {
      description.appendText("reportLogcat: " + config.isLogcatReportingEnabled());
      boolean includeVersionCode = config.isVersionCodeToBeIncludedInVersionString();
      description.appendText(", includeVersionCode: " + includeVersionCode);
      description.appendText(", customVersionName: " + config.getCustomVersionName());
      description.appendText(", enableServiceMonitoring: " + config.isServiceMonitoringEnabled());
    }
  }

  private static class JSONObjectMatcher extends TypeSafeMatcher<JSONObject> {
    private final JSONObject expected;

    private JSONObjectMatcher(JSONObject expected) {
      this.expected = expected;
    }

    @Override public boolean matchesSafely(JSONObject jsonObject) {
      // todo: this relies on having the same order
      return expected.toString().equals(jsonObject.toString());
    }

    @Override public void describeTo(Description description) {
      description.appendText(expected.toString());
    }
  }
}