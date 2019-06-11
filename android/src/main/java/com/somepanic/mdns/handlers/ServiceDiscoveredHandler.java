package com.somepanic.mdns.handlers;

import android.net.nsd.NsdServiceInfo;
import io.flutter.plugin.common.EventChannel;
import android.os.Handler;
import android.os.Looper;

import java.util.HashMap;
import java.util.Map;

public class ServiceDiscoveredHandler implements EventChannel.StreamHandler {

    EventChannel.EventSink sink;
    @Override
    public void onListen(Object o, EventChannel.EventSink eventSink) {
        sink = eventSink;
    }

    @Override
    public void onCancel(Object o) {

    }

    public void onServiceDiscovered(final Map<String, Object> serviceInfoMap){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run () {
                sink.success(serviceInfoMap);
            }
        });
    }
}