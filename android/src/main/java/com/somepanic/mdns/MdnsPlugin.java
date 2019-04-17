package com.somepanic.mdns;

import android.content.Context;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import com.github.druk.rxdnssd.BonjourService;
import com.github.druk.rxdnssd.RxDnssd;
import com.github.druk.rxdnssd.RxDnssdBindable;
import com.github.druk.rxdnssd.RxDnssdEmbedded;
import android.util.Log;
import android.os.Build;
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

import java.util.concurrent.Semaphore;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * MdnsPlugin
 */
public class MdnsPlugin implements MethodCallHandler {

    private final String TAG = getClass().getSimpleName();
    private final static String NAMESPACE = "com.somepanic.mdns";

    private static RxDnssd mDnssd;
    private Subscription mBrowseSubscription;

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

        if (Build.VERSION.RELEASE.contains("4.4.2") && Build.MANUFACTURER.toLowerCase().contains("samsung")){
            Log.i(TAG, "Using embedded version of dns sd because of Samsung 4.4.2");
            mDnssd = new RxDnssdEmbedded(r.context());
        } else {
            mDnssd = new RxDnssdBindable(r.context());
        }
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

    private void _startDiscovery(String serviceName) {
        stopDiscovery(null, null);

        mBrowseSubscription = mDnssd.browse(serviceName, "local.")
            .compose(mDnssd.resolve())
            .compose(mDnssd.queryIPRecords())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<BonjourService>() {
                @Override
                public void call(BonjourService bonjourService) {
                    if (bonjourService.isLost()) {
                        Log.d(TAG, "Lost Service : " + bonjourService.toString());
                        mLostHandler.onServiceLost(ServiceToMap(bonjourService));
                    } else {
                        Log.d(TAG, "Found Service : " + bonjourService.toString());
                        mDiscoveredHandler.onServiceDiscovered(ServiceToMap(bonjourService));
                        mResolvedHandler.onServiceResolved(ServiceToMap(bonjourService));
                    }
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.e(TAG, "error", throwable);
                }
            });
    }

    private void stopDiscovery(MethodCall call, Result result){
        if (mBrowseSubscription != null)
            mBrowseSubscription.unsubscribe();
        mBrowseSubscription = null;
    }

    /**
     * serviceToMap converts an NsdServiceInfo object into a map of relevant info
     * The map can be interpreted by the StandardMessageCodec of Flutter and makes sending data back and forth simpler.
     * @param info The ServiceInfo to convert
     * @return The map that can be interpreted by Flutter and sent back on an EventChannel
     */
    private static Map<String, Object> ServiceToMap(BonjourService info) {
        Map<String, Object> map = new HashMap<>();

        map.put("name", info.getServiceName() != null ? info.getServiceName() : "");
        map.put("type", info.getRegType() != null ? info.getRegType() : "");
        map.put("host", "/" + info.getInet4Address() != null ? info.getInet4Address().getHostAddress() : "");
        map.put("port", info.getPort());

        map.put("txtRecords", info.getTxtRecords());

        return map;
    }
}
