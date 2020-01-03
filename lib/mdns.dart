import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';

class ServiceInfo{
  String name;
  String type;
  String host;
  int port;
  Map<dynamic, dynamic> txtRecords;
  ServiceInfo(this.name, this.type, this.host, this.port, this.txtRecords);

  static ServiceInfo fromMap(Map fromChannel){
    String name = "";
    String type = "";
    String host = "";
    int port = 0;
    Map <dynamic, dynamic> txtRecords;

    if ( fromChannel.containsKey("name") ) {
      name = fromChannel["name"];
    }

    if (fromChannel.containsKey("type")) {
      type = fromChannel["type"];
    }

    if (fromChannel.containsKey("host")) {
      host = fromChannel["host"];
    }

    if (fromChannel.containsKey("port")) {
      port = fromChannel["port"];
    }

    if (fromChannel.containsKey("txtRecords")) {
      txtRecords = fromChannel["txtRecords"];
    }

    return new ServiceInfo(name, type, host, port, txtRecords);
  }

  @override
  String toString(){
    return "Name: $name, Type: $type, Host: $host, Port: $port, txtRecords: $txtRecords";
  }
}
typedef void ServiceInfoCallback(ServiceInfo info);

typedef void IntCallback (int data);
typedef void VoidCallback();

class DiscoveryCallbacks{
  VoidCallback onDiscoveryStarted;
  VoidCallback onDiscoveryStopped;
  ServiceInfoCallback onDiscovered;
  ServiceInfoCallback onResolved;
  ServiceInfoCallback onLost;

  DiscoveryCallbacks({
    this.onDiscoveryStarted,
    this.onDiscoveryStopped,
    this.onDiscovered,
    this.onResolved,
    this.onLost,
  });
}

class AdvertiseCallbacks{
  VoidCallback onAdvertisingStarted;
  VoidCallback onAdvertisingStopped;

  AdvertiseCallbacks ({
    this.onAdvertisingStarted,
    this.onAdvertisingStopped,
  });
}

class Mdns {
  static const String NAMESPACE = "com.somepanic.mdns";

  final MethodChannel _channel =
  const MethodChannel('$NAMESPACE/mdns');

  final EventChannel _serviceDiscoveredChannel =
  const EventChannel("$NAMESPACE/discovered");

  final EventChannel _serviceResolvedChannel =
  const EventChannel("$NAMESPACE/resolved");

  final EventChannel _serviceLostChannel =
  const EventChannel("$NAMESPACE/lost");

  final EventChannel _discoveryRunningChannel =
  const EventChannel("$NAMESPACE/running");

  DiscoveryCallbacks discoveryCallbacks;
  AdvertiseCallbacks advertiseCallbacks;
  Mdns({this.discoveryCallbacks, this.advertiseCallbacks}){

    if ( discoveryCallbacks != null ) {
      WidgetsFlutterBinding.ensureInitialized();

      //Configure all the discovery related callbacks and event channels
      _serviceDiscoveredChannel.receiveBroadcastStream().listen((data) {
        print("Service discovered ${data.toString()}");
        if (discoveryCallbacks.onDiscovered != null) {
          discoveryCallbacks.onDiscovered(ServiceInfo.fromMap(data));
        }
      });

      _serviceResolvedChannel.receiveBroadcastStream().listen((data) {
        print("Service resolved ${data.toString()}");
        if (discoveryCallbacks.onResolved != null) {
          discoveryCallbacks.onResolved(ServiceInfo.fromMap(data));
        }
      });

      _serviceLostChannel.receiveBroadcastStream().listen((data) {
        print("Service lost ${data.toString()}");
        if (discoveryCallbacks.onLost != null) {
          discoveryCallbacks.onLost(ServiceInfo.fromMap(data));
        }
      });

      _discoveryRunningChannel.receiveBroadcastStream().listen((running) {
        print("Discovery Running? $running");
        if (running && discoveryCallbacks.onDiscoveryStarted != null) {
          discoveryCallbacks.onDiscoveryStarted();
        } else if (discoveryCallbacks.onDiscoveryStopped != null) {
          discoveryCallbacks.onDiscoveryStopped();
        }
      });
    }

    if (advertiseCallbacks != null) {
      //TODO advertise stuff
    }
  }

  Mdns startDiscovery(String serviceType) {
    Map args = new Map();
    args["serviceType"] = serviceType;
    _channel.invokeMethod("startDiscovery", args);
    return this;
  }

  Mdns stopDiscovery(){
    _channel.invokeMethod("stopDiscovery", new Map());
    return this;
  }

  addService(String serviceName) {
    //TODO advertising
  }

}
