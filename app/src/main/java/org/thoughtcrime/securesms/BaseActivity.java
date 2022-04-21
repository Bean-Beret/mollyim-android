package org.thoughtcrime.securesms;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;

import org.signal.core.util.logging.Log;
import org.thoughtcrime.securesms.dependencies.ApplicationDependencies;
import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.util.AppStartup;
import org.thoughtcrime.securesms.util.ConfigurationUtil;
import org.thoughtcrime.securesms.util.ServiceUtil;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.dynamiclanguage.DynamicLanguageContextWrapper;

import java.util.List;
import java.util.Objects;

/**
 * Base class for all activities. The vast majority of activities shouldn't extend this directly.
 * Instead, they should extend {@link PassphraseRequiredActivity} so they're protected by
 * screen lock.
 */
public abstract class BaseActivity extends AppCompatActivity {
  private static final String TAG = Log.tag(BaseActivity.class);

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    AppStartup.getInstance().onCriticalRenderEventStart();
    logEvent("onCreate()");
    super.onCreate(savedInstanceState);
    AppStartup.getInstance().onCriticalRenderEventEnd();
  }

  @Override
  protected void onResume() {
    super.onResume();
    initializeScreenshotSecurity();
  }

  @Override
  protected void onStart() {
    logEvent("onStart()");
    if (ApplicationDependencies.isInitialized()) {
      ApplicationDependencies.getShakeToReport().registerActivity(this);
    }
    super.onStart();
  }

  @Override
  protected void onStop() {
    logEvent("onStop()");
    super.onStop();
  }

  @Override
  protected void onDestroy() {
    logEvent("onDestroy()");
    super.onDestroy();
  }

  private void initializeScreenshotSecurity() {
    if (KeyCachingService.isLocked() || TextSecurePreferences.isScreenSecurityEnabled(this)) {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
  }

  @RequiresApi(21)
  protected void setExcludeFromRecents(boolean exclude) {
    int taskId = getTaskId();

    List<ActivityManager.AppTask> tasks = ServiceUtil.getActivityManager(this).getAppTasks();
    for (ActivityManager.AppTask task : tasks) {
      if (task.getTaskInfo().id == taskId) {
        task.setExcludeFromRecents(exclude);
      }
    }
  }

  protected void startActivitySceneTransition(Intent intent, View sharedView, String transitionName) {
    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this, sharedView, transitionName)
                                         .toBundle();
    ActivityCompat.startActivity(this, intent, bundle);
  }

  @Override
  protected void attachBaseContext(@NonNull Context newBase) {
    super.attachBaseContext(newBase);

    Configuration configuration      = new Configuration(newBase.getResources().getConfiguration());
    int           appCompatNightMode = getDelegate().getLocalNightMode() != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED ? getDelegate().getLocalNightMode()
                                                                                                                     : AppCompatDelegate.getDefaultNightMode();

    configuration.uiMode      = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | mapNightModeToConfigurationUiMode(newBase, appCompatNightMode);
    configuration.orientation = Configuration.ORIENTATION_UNDEFINED;

    applyOverrideConfiguration(configuration);
  }

  @Override
  public void applyOverrideConfiguration(@NonNull Configuration overrideConfiguration) {
    DynamicLanguageContextWrapper.prepareOverrideConfiguration(this, overrideConfiguration);
    super.applyOverrideConfiguration(overrideConfiguration);
  }

  // MOLLY: Workaround for WebView crashes in devices API 21-25 with outdated WebView
  // https://issuetracker.google.com/issues/141351441
  // Not reproducible in Signal because RegistrationNavigationActivity does not inherit
  // this class and thus does not override the configuration.
  @Override
  public AssetManager getAssets() {
    if (Build.VERSION.SDK_INT <= 25) {
      return getResources().getAssets();  // Ignore overridden configuration
    } else {
      return super.getAssets();
    }
  }

  private void logEvent(@NonNull String event) {
    Log.d(TAG, "[" + Log.tag(getClass()) + "] " + event);
  }

  public final @NonNull ActionBar requireSupportActionBar() {
    return Objects.requireNonNull(getSupportActionBar());
  }

  private static int mapNightModeToConfigurationUiMode(@NonNull Context context, @AppCompatDelegate.NightMode int appCompatNightMode) {
    if (appCompatNightMode == AppCompatDelegate.MODE_NIGHT_YES) {
      return Configuration.UI_MODE_NIGHT_YES;
    } else if (appCompatNightMode == AppCompatDelegate.MODE_NIGHT_NO) {
      return Configuration.UI_MODE_NIGHT_NO;
    }
    return ConfigurationUtil.getNightModeConfiguration(context.getApplicationContext());
  }
}
