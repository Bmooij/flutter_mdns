import Flutter
import UIKit

public class SwiftMdnsPlugin: NSObject, FlutterPlugin, NetServiceBrowserDelegate, NetServiceDelegate {
    var discovered : FlutterEventSink;
    var resolved : FlutterEventSink;
    var lost : FlutterEventSink;
    var running : FlutterEventSink;
    var netServiceBrowser : NetServiceBrowser;

    override init() {
        discovered = SwiftMdnsPlugin.dummy;
        resolved = SwiftMdnsPlugin.dummy;
        lost = SwiftMdnsPlugin.dummy;
        running = SwiftMdnsPlugin.dummy;
        netServiceBrowser = NetServiceBrowser();
    }

    static func dummy(_ : Optional<Any>) -> () {    }

    public static func register(with registrar: FlutterPluginRegistrar) {
        let NAMESPACE = "com.somepanic.mdns";
        
        let channel = FlutterMethodChannel.init(name: NAMESPACE + "/mdns", binaryMessenger: registrar.messenger())
        let instance = SwiftMdnsPlugin()

        class ServiceHandler : NSObject {
            var plugin : SwiftMdnsPlugin;
            init(pluginInstance : SwiftMdnsPlugin) {
                plugin = pluginInstance;
            }
        }
        class ServiceDiscoveredHandler : ServiceHandler, FlutterStreamHandler {
            override init(pluginInstance:SwiftMdnsPlugin) {
                super.init(pluginInstance: pluginInstance);
            }
            func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
                NSLog("onListen discover");
                plugin.discovered = events;
                return nil;
            }

            func onCancel(withArguments arguments: Any?) -> FlutterError? {
                NSLog("onCancel discover");
                plugin.discovered = SwiftMdnsPlugin.dummy;
                return nil;
            }
        }

        class ServiceResolvedHandler : ServiceHandler, FlutterStreamHandler {
            override init(pluginInstance:SwiftMdnsPlugin) {
                super.init(pluginInstance: pluginInstance);
            }
            func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
                NSLog("onListen resolved");
                plugin.resolved = events;
                return nil;
            }

            func onCancel(withArguments arguments: Any?) -> FlutterError? {
                NSLog("onCancel resolved");
                plugin.resolved = SwiftMdnsPlugin.dummy;
                return nil;
            }
        }
        class ServiceLostHandler : ServiceHandler, FlutterStreamHandler {
            override init(pluginInstance:SwiftMdnsPlugin) {
                super.init(pluginInstance: pluginInstance);
            }
            func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
                NSLog("onListen lost");
                plugin.lost = events;
                return nil;
            }

            func onCancel(withArguments arguments: Any?) -> FlutterError? {
                NSLog("onCancel lost");
                plugin.lost = SwiftMdnsPlugin.dummy;
                return nil;
            }
        }
        class ServiceRunningHandler : ServiceHandler, FlutterStreamHandler {
            override init(pluginInstance:SwiftMdnsPlugin) {
                super.init(pluginInstance: pluginInstance);
            }
            func onListen(withArguments arguments: Any?, eventSink events: @escaping FlutterEventSink) -> FlutterError? {
                NSLog("onListen running");
                plugin.running = events;
                return nil;
            }

            func onCancel(withArguments arguments: Any?) -> FlutterError? {
                NSLog("onCancel running");
                plugin.running = SwiftMdnsPlugin.dummy;
                return nil;
            }
        }

        eventChannelWithHandler(name: NAMESPACE + "/discovered", registrar: registrar, handler: ServiceDiscoveredHandler(pluginInstance: instance));
        eventChannelWithHandler(name: NAMESPACE + "/resolved", registrar: registrar, handler: ServiceResolvedHandler(pluginInstance: instance));
        eventChannelWithHandler(name: NAMESPACE + "/lost", registrar: registrar, handler: ServiceLostHandler(pluginInstance: instance));
        eventChannelWithHandler(name: NAMESPACE + "/running", registrar: registrar, handler: ServiceRunningHandler(pluginInstance: instance));
        registrar.addMethodCallDelegate(instance, channel: channel)
    }

    static func eventChannelWithHandler<T: FlutterStreamHandler & NSObjectProtocol>(name: String, registrar: FlutterPluginRegistrar, handler: T) -> () {
        let res = FlutterEventChannel(name: name, binaryMessenger: registrar.messenger());
        res.setStreamHandler(handler);
    }

    public func netServiceBrowserWillSearch(_ browser: NetServiceBrowser) {
        running(true);
    }
    public func netServiceBrowserDidStopSearch(_ browser: NetServiceBrowser) {
        running(false);
    }
    
    var services = Set<NetService>();
    public func netServiceBrowser(_ browser: NetServiceBrowser, didFind service: NetService, moreComing: Bool) {
        services.insert(service);
        service.delegate = self;
        service.resolve(withTimeout: TimeInterval(10));
    }

    public func netServiceBrowser(_ browser: NetServiceBrowser,
                                  didRemove service: NetService,
                                  moreComing: Bool) {
        services.remove(service);
        lost(serviceToMap(service: service));
    }

    private func ipString(from: [Data]) -> String {
        let theAddress = from.first! as NSData;
        var hostname = [CChar](repeating: 0, count: Int(NI_MAXHOST))
        if getnameinfo(theAddress.bytes.assumingMemoryBound(to: sockaddr.self), socklen_t(theAddress.length), &hostname, socklen_t(hostname.count), nil, 0, NI_NUMERICHOST) == 0 {
            return String(cString: hostname)
        }
        return "";
    }
    
    private func serviceToMap(service: NetService) -> Dictionary<String, Any> {
        var res = [String:Any]()
        res["name"] = service.name
        res["type"] = service.type
        if let addresses = service.addresses {
            res["host"] = "/" + ipString(from: addresses);
        }
        res["port"] = service.port
        return res
    }

    public func netServiceDidResolveAddress(_ sender: NetService) {
        resolved(serviceToMap(service: sender));
        services.remove(sender);
    }

    public func netService(_ sender: NetService,
                           didNotResolve errorDict: [String : NSNumber]) {
        services.remove(sender);
    }

    public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
        switch (call.method) {
        case "startDiscovery":
            let serviceType = (call.arguments as! Dictionary<String, Any>)["serviceType"] as! String;
            netServiceBrowser.delegate = self;
            netServiceBrowser.searchForServices(ofType: serviceType, inDomain: "");
            result(nil);
            break;
        case "stopDiscovery":
            netServiceBrowser.stop();
            result(nil);
            break;
        default:
            NSLog("Cannot handle %@", call.method);
            break;
        }
    }

}
