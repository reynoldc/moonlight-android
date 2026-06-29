package com.limelight.binding.input.capture;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Build;

import java.util.Locale;

import com.limelight.BuildConfig;
import com.limelight.LimeLog;
import com.limelight.R;
import com.limelight.binding.input.evdev.EvdevCaptureProviderShim;
import com.limelight.binding.input.evdev.EvdevListener;
import com.limelight.preferences.PreferenceConfiguration;

public class InputCaptureManager {
    private static boolean shouldDisableNativePointerCapture(Activity activity) {
        if (!PreferenceConfiguration.readPreferences(activity).enableXiaomiTouchpadCompat) {
            return false;
        }

        String manufacturer = String.valueOf(Build.MANUFACTURER).toLowerCase(Locale.US);
        String brand = String.valueOf(Build.BRAND).toLowerCase(Locale.US);
        String model = String.valueOf(Build.MODEL).toLowerCase(Locale.US);
        boolean isXiaomiFamilyDevice = manufacturer.contains("xiaomi") || brand.contains("xiaomi") ||
                model.contains("xiaomi") ||
                manufacturer.contains("redmi") || brand.contains("redmi") ||
                model.contains("redmi") ||
                manufacturer.contains("poco") || brand.contains("poco") ||
                model.contains("poco");
        Configuration configuration = activity.getResources().getConfiguration();
        boolean isTablet = configuration.smallestScreenWidthDp >= 600;

        return isXiaomiFamilyDevice && isTablet;
    }

    public static InputCaptureProvider getInputCaptureProvider(Activity activity, EvdevListener rootListener) {
        boolean disableNativePointerCapture = shouldDisableNativePointerCapture(activity);

        if (AndroidNativePointerCaptureProvider.isCaptureProviderSupported() &&
                !disableNativePointerCapture) {
            LimeLog.info("Using Android O+ native mouse capture");
            return new AndroidNativePointerCaptureProvider(activity, activity.findViewById(R.id.surfaceView));
        }
        else if (AndroidPointerIconCaptureProvider.isCaptureProviderSupported() &&
                disableNativePointerCapture) {
            // HyperOS on Xiaomi tablets already maps trackpad gestures to scroll and right click.
            // Native pointer capture converts those gestures into relative mouse input, so prefer
            // pointer icon hiding only and let the framework deliver the touchpad events directly.
            LimeLog.info("Using Android N+ pointer hiding to preserve Xiaomi tablet touchpad gestures");
            return new AndroidPointerIconCaptureProvider(activity, activity.findViewById(R.id.surfaceView));
        }
        // LineageOS implemented broken NVIDIA capture extensions, so avoid using them on root builds.
        // See https://github.com/LineageOS/android_frameworks_base/commit/d304f478a023430f4712dbdc3ee69d9ad02cebd3
        else if (!BuildConfig.ROOT_BUILD && ShieldCaptureProvider.isCaptureProviderSupported()) {
            LimeLog.info("Using NVIDIA mouse capture extension");
            return new ShieldCaptureProvider(activity);
        }
        else if (EvdevCaptureProviderShim.isCaptureProviderSupported()) {
            LimeLog.info("Using Evdev mouse capture");
            return EvdevCaptureProviderShim.createEvdevCaptureProvider(activity, rootListener);
        }
        else if (AndroidPointerIconCaptureProvider.isCaptureProviderSupported()) {
            // Android N's native capture can't capture over system UI elements
            // so we want to only use it if there's no other option.
            LimeLog.info("Using Android N+ pointer hiding");
            return new AndroidPointerIconCaptureProvider(activity, activity.findViewById(R.id.surfaceView));
        }
        else {
            LimeLog.info("Mouse capture not available");
            return new NullCaptureProvider();
        }
    }
}
