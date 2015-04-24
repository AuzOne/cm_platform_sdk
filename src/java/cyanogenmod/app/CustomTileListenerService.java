/**
 * Copyright (c) 2015, The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cyanogenmod.app;

import android.annotation.SdkConstant;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import cyanogenmod.app.ICustomTileListener;
import cyanogenmod.app.ICMStatusBarManager;

import org.cyanogenmod.internal.statusbar.IStatusBarCustomTileHolder;

/**
 * A service that receives calls from the system when new custom tiles are
 * posted or removed.
 * <p>To extend this class, you must declare the service in your manifest file with
 * the TODO: add permission
 * and include an intent filter with the {@link #SERVICE_INTERFACE} action. For example:</p>
 * <pre>
 * &lt;service android:name=".CustomTileListener"
 *          android:label="&#64;string/service_name"
 *          android:permission="TODO: Add me">
 *     &lt;intent-filter>
 *         &lt;action android:name="cyanogenmod.app.CustomTileListenerService" />
 *     &lt;/intent-filter>
 * &lt;/service></pre>
 */
public class CustomTileListenerService extends Service {
    private final String TAG = CustomTileListenerService.class.getSimpleName()
            + "[" + getClass().getSimpleName() + "]";
    /**
     * The {@link android.content.Intent} that must be declared as handled by the service.
     */
    @SdkConstant(SdkConstant.SdkConstantType.SERVICE_ACTION)
    public static final String SERVICE_INTERFACE
            = "cyanogenmod.app.CustomTileListenerService";

    private ICustomTileListenerWrapper mWrapper = null;
    private ICMStatusBarManager mStatusBarService;
    /** Only valid after a successful call to (@link registerAsService}. */
    private int mCurrentUser;

    @Override
    public IBinder onBind(Intent intent) {
        if (mWrapper == null) {
            mWrapper = new ICustomTileListenerWrapper();
        }
        return mWrapper;
    }

    private final ICMStatusBarManager getStatusBarInterface() {
        if (mStatusBarService == null) {
            mStatusBarService = ICMStatusBarManager.Stub.asInterface(
                    ServiceManager.getService(CMContextConstants.CM_STATUS_BAR_SERVICE));
        }
        return mStatusBarService;
    }

    /**
     * Directly register this service with the StatusBar Manager.
     *
     * <p>Only system services may use this call. It will fail for non-system callers.
     * Apps should ask the user to add their listener in Settings.
     *
     * @param context Context required for accessing resources. Since this service isn't
     *    launched as a real Service when using this method, a context has to be passed in.
     * @param componentName the component that will consume the custom tile information
     * @param currentUser the user to use as the stream filter
     * @hide
     */
    public void registerAsSystemService(Context context, ComponentName componentName,
                                        int currentUser) throws RemoteException {
        if (mWrapper == null) {
            mWrapper = new ICustomTileListenerWrapper();
        }
        ICMStatusBarManager statusBarInterface = getStatusBarInterface();
        statusBarInterface.registerListener(mWrapper, componentName, currentUser);
        mCurrentUser = currentUser;
    }

    /**
     * Directly unregister this service from the StatusBar Manager.
     *
     * <P>This method will fail for listeners that were not registered
     * with (@link registerAsService).
     * @hide
     */
    public void unregisterAsSystemService() throws RemoteException {
        if (mWrapper != null) {
            ICMStatusBarManager statusBarInterface = getStatusBarInterface();
            statusBarInterface.unregisterListener(mWrapper, mCurrentUser);
        }
    }


    private class ICustomTileListenerWrapper extends ICustomTileListener.Stub {
        @Override
        public void onListenerConnected() {
            synchronized (mWrapper) {
                try {
                    CustomTileListenerService.this.onListenerConnected();
                } catch (Throwable t) {
                    Log.w(TAG, "Error running onListenerConnected", t);
                }
            }
        }
        @Override
        public void onCustomTilePosted(IStatusBarCustomTileHolder sbcHolder) {
            StatusBarPanelCustomTile sbc;
            try {
                sbc = sbcHolder.get();
            } catch (RemoteException e) {
                Log.w(TAG, "onCustomTilePosted: Error receiving StatusBarPanelCustomTile", e);
                return;
            }
            synchronized (mWrapper) {
                try {
                    CustomTileListenerService.this.onCustomTilePosted(sbc);
                } catch (Throwable t) {
                    Log.w(TAG, "Error running onCustomTilePosted", t);
                }
            }
        }
        @Override
        public void onCustomTileRemoved(IStatusBarCustomTileHolder sbcHolder) {
            StatusBarPanelCustomTile sbc;
            try {
                sbc = sbcHolder.get();
            } catch (RemoteException e) {
                Log.w(TAG, "onCustomTileRemoved: Error receiving StatusBarPanelCustomTile", e);
                return;
            }
            synchronized (mWrapper) {
                try {
                    CustomTileListenerService.this.onCustomTileRemoved(sbc);
                } catch (Throwable t) {
                    Log.w(TAG, "Error running onCustomTileRemoved", t);
                }
            }
        }
    }

    /**
     * Implement this method to learn about new custom tiles as they are posted by apps.
     *
     * @param sbc A data structure encapsulating the original {@link cyanogenmod.app.CustomTile}
     *            object as well as its identifying information (tag and id) and source
     *            (package name).
     */
    public void onCustomTilePosted(StatusBarPanelCustomTile sbc) {
        // optional
    }

    /**
     * Implement this method to learn when custom tiles are removed.
     *
     * @param sbc A data structure encapsulating at least the original information (tag and id)
     *            and source (package name) used to post the {@link cyanogenmod.app.CustomTile} that
     *            was just removed.
     */
    public void onCustomTileRemoved(StatusBarPanelCustomTile sbc) {
        // optional
    }

    /**
     * Implement this method to learn about when the listener is enabled and connected to
     * the status bar manager.
     * at this time.
     */
    public void onListenerConnected() {
        // optional
    }
}