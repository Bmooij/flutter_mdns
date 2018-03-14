import 'package:flutter/material.dart';
import 'package:mdns/mdns.dart';

void main() => runApp(new MyApp());

const String discovery_service = "_workstation._tcp";

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => new _MyAppState();
}

class _MyAppState extends State<MyApp> {
  List<String> messageLog = <String>[];
  DiscoveryCallbacks discoveryCallbacks;
  @override
  initState() {
    super.initState();

    discoveryCallbacks = new DiscoveryCallbacks(
      onDiscoveryStarted: (){
        print("Discovery started");
        setState((){messageLog.insert(0, "DISCOVERY: Discovery Running");});
      },
      onDiscoveryStopped: (){
        print("Discovery stopped");
        setState((){messageLog.insert(0, "DISCOVERY: Discovery Not Running");});
      },
      onDiscovered: (ServiceInfo info){
        print("Discovered ${info.toString()}");
        setState((){
          messageLog.insert(0, "DISCOVERY: Discovered ${info.toString()}");
        });
      },
      onResolved: (ServiceInfo info){
        print("Resolved Service ${info.toString()}");
        setState((){
          messageLog.insert(0, "DISCOVERY: Resolved ${info.toString()}");
        });
      },
      onLost: (ServiceInfo info) {
        print("Lost Service ${info.toString()}");
        setState((){
          messageLog.insert(0, "DISCOVERY: Lost ${info.toString()}");
        });
      }
    );


    messageLog.add("Starting mDNS for service [$discovery_service]");
    startMdnsDiscovery(discovery_service);
  }

  startMdnsDiscovery(String serviceType){
    Mdns mdns = new Mdns(discoveryCallbacks: discoveryCallbacks);
    mdns.startDiscovery(serviceType);
  }

  @override
  Widget build(BuildContext context) {

    return new MaterialApp(
      home: new Scaffold(
        body: new ListView.builder(
          reverse: true,
          itemCount: messageLog.length,
          itemBuilder: (BuildContext context, int index) {
            return new Text(messageLog[index]);
          },
        )
      ),
    );
  }
}
