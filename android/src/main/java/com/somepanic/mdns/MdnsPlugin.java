package com.somepanic.mdns;

import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.util.Log;
import com.somepanic.mdns.handlers.DiscoveryRunningHandler;
import com.somepanic.mdns.handlers.ServiceDiscoveredHandler;
import com.somepanic.mdns.handlers.ServiceLostHandler;
import com.somepanic.mdns.handlers.ServiceResolvedHandler;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MdnsPlugin
 */
public class MdnsPlugin implements MethodCallHandler {

    private final String TAG = getClass().getSimpleName();
    private final static String NAMESPACE = "com.somepanic.mdns";

    private NsdManager mNsdManager;
    private NsdManager.DiscoveryListener mDiscoveryListener;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MdnsPlugin instance = new MdnsPlugin(registrar);
    }

    private Registrar mRegistrar;
    private DiscoveryRunningHandler mDiscoveryRunningHandler;
    private ServiceDiscoveredHandler mDiscoveredHandler;
    private ServiceResolvedHandler mResolvedHandler;
    private ServiceLostHandler mLostHandler;

    MdnsPlugin(Registrar r) {

        EventChannel serviceDiscoveredChannel = new EventChannel(r.messenger(), NAMESPACE + "/discovered");
        mDiscoveredHandler = new ServiceDiscoveredHandler();
        serviceDiscoveredChannel.setStreamHandler(mDiscoveredHandler);

        EventChannel serviceResolved = new EventChannel(r.messenger(), NAMESPACE + "/resolved");
        mResolvedHandler = new ServiceResolvedHandler();
        serviceResolved.setStreamHandler(mResolvedHandler);

        EventChannel serviceLost = new EventChannel(r.messenger(), NAMESPACE + "/lost");
        mLostHandler = new ServiceLostHandler();
        serviceLost.setStreamHandler(mLostHandler);

        EventChannel discoveryRunning = new EventChannel(r.messenger(), NAMESPACE + "/running");
        mDiscoveryRunningHandler = new DiscoveryRunningHandler();
        discoveryRunning.setStreamHandler(mDiscoveryRunningHandler);

        final MethodChannel channel = new MethodChannel(r.messenger(), NAMESPACE + "/mdns");
        channel.setMethodCallHandler(this);

        mRegistrar = r;
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "startDiscovery":
                startDiscovery(call, result);
                break;
            case "stopDiscovery" :
                stopDiscovery(call, result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private final static String SERVICE_KEY = "serviceType";
    private void startDiscovery(MethodCall call, Result result) {
        if (call.hasArgument(SERVICE_KEY)){

            String service = call.argument(SERVICE_KEY);
            _startDiscovery(service);

            result.success(null);
        } else {
            result.error("Not Enough Arguments", "Expected: String serviceType", null);
        }
    }

    private void _startDiscovery(String serviceName){

        mNsdManager = (NsdManager)mRegistrar.activity().getSystemService(Context.NSD_SERVICE);

        mDiscoveryListener = new NsdManager.DiscoveryListener(){

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, String.format(Locale.US,
                        "Discovery failed to start on %s with error : %d", serviceType, errorCode));
                mDiscoveryRunningHandler.onDiscoveryStopped();
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, String.format(Locale.US,
                        "Discovery failed to stop on %s with error : %d", serviceType, errorCode));
                mDiscoveryRunningHandler.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryStarted(String serviceType) {
                Log.d(TAG, "Started discovery for : " + serviceType);
                mDiscoveryRunningHandler.onDiscoveryStarted();
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.d(TAG, "Stopped discovery for : " + serviceType);
                mDiscoveryRunningHandler.onDiscoveryStopped();
            }

            @Override
            public void onServiceFound(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "Found Service : " + nsdServiceInfo.toString());
                mDiscoveredHandler.onServiceDiscovered(ServiceToMap(nsdServiceInfo));

                mNsdManager.resolveService(nsdServiceInfo, new NsdManager.ResolveListener() {
                    @Override
                    public void onResolveFailed(NsdServiceInfo nsdServiceInfo, int i) {
                        Log.d(TAG, "Failed to resolve service : " + nsdServiceInfo.toString());
                    }

                    @Override
                    public void onServiceResolved(NsdServiceInfo nsdServiceInfo) {
                        mResolvedHandler.onServiceResolved(ServiceToMap(nsdServiceInfo));
                    }
                });
            }

            @Override
            public void onServiceLost(NsdServiceInfo nsdServiceInfo) {
                Log.d(TAG, "Lost Service : " + nsdServiceInfo.toString());
                mLostHandler.onServiceLost(ServiceToMap(nsdServiceInfo));
            }
        };

        mNsdManager.discoverServices(serviceName, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);
    }

    private void stopDiscovery(MethodCall call, Result result){
        if (mNsdManager != null && mDiscoveryListener != null) {
            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        } else {
            result.error("IllegalState", "NetworkDiscovery is not running", null);
        }
    }

    /**
     * serviceToMap converts an NsdServiceInfo object into a map of relevant info
     * The map can be interpreted by the StandardMessageCodec of Flutter and makes sending data back and forth simpler.
     * @param info The ServiceInfo to convert
     * @return The map that can be interpreted by Flutter and sent back on an EventChannel
     */
    private static Map<String, Object> ServiceToMap(NsdServiceInfo info) {
        Map<String, Object> map = new HashMap<>();

        map.put("name", info.getServiceName() != null ? info.getServiceName() : "");
        map.put("type", info.getServiceType() != null ? info.getServiceType() : "");
        map.put("host", info.getHost() != null ? info.getHost().toString() : "");
        map.put("port", info.getPort());

        return map;
    }
}
