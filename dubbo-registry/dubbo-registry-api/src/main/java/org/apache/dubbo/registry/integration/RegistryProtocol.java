/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.dubbo.registry.integration;

import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.URLBuilder;
import org.apache.dubbo.common.config.ConfigurationUtils;
import org.apache.dubbo.common.config.configcenter.DynamicConfiguration;
import org.apache.dubbo.common.extension.ExtensionLoader;
import org.apache.dubbo.common.logger.Logger;
import org.apache.dubbo.common.logger.LoggerFactory;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.common.utils.NamedThreadFactory;
import org.apache.dubbo.common.utils.StringUtils;
import org.apache.dubbo.common.utils.UrlUtils;
import org.apache.dubbo.registry.NotifyListener;
import org.apache.dubbo.registry.Registry;
import org.apache.dubbo.registry.RegistryFactory;
import org.apache.dubbo.registry.RegistryService;
import org.apache.dubbo.rpc.Exporter;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Protocol;
import org.apache.dubbo.rpc.ProtocolServer;
import org.apache.dubbo.rpc.ProxyFactory;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.Cluster;
import org.apache.dubbo.rpc.cluster.Configurator;
import org.apache.dubbo.rpc.cluster.governance.GovernanceRuleRepository;
import org.apache.dubbo.rpc.model.ApplicationModel;
import org.apache.dubbo.rpc.model.ProviderModel;
import org.apache.dubbo.rpc.protocol.InvokerWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.apache.dubbo.common.constants.CommonConstants.APPLICATION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.CLUSTER_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.COMMA_SPLIT_PATTERN;
import static org.apache.dubbo.common.constants.CommonConstants.DUBBO_VERSION_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.EXTRA_KEYS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.GROUP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.HIDE_KEY_PREFIX;
import static org.apache.dubbo.common.constants.CommonConstants.INTERFACE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.LOADBALANCE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.METHODS_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.MONITOR_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.PATH_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.RELEASE_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMEOUT_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.TIMESTAMP_KEY;
import static org.apache.dubbo.common.constants.CommonConstants.VERSION_KEY;
import static org.apache.dubbo.common.constants.FilterConstants.VALIDATION_KEY;
import static org.apache.dubbo.common.constants.QosConstants.ACCEPT_FOREIGN_IP;
import static org.apache.dubbo.common.constants.QosConstants.QOS_ENABLE;
import static org.apache.dubbo.common.constants.QosConstants.QOS_HOST;
import static org.apache.dubbo.common.constants.QosConstants.QOS_PORT;
import static org.apache.dubbo.common.constants.RegistryConstants.CATEGORY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.CONFIGURATORS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.OVERRIDE_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.PROVIDERS_CATEGORY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_KEY;
import static org.apache.dubbo.common.constants.RegistryConstants.REGISTRY_PROTOCOL;
import static org.apache.dubbo.common.constants.RegistryConstants.ROUTERS_CATEGORY;
import static org.apache.dubbo.common.utils.UrlUtils.classifyUrls;
import static org.apache.dubbo.registry.Constants.CONFIGURATORS_SUFFIX;
import static org.apache.dubbo.registry.Constants.CONSUMER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.DEFAULT_REGISTRY;
import static org.apache.dubbo.registry.Constants.PROVIDER_PROTOCOL;
import static org.apache.dubbo.registry.Constants.REGISTER_IP_KEY;
import static org.apache.dubbo.registry.Constants.REGISTER_KEY;
import static org.apache.dubbo.registry.Constants.SIMPLIFIED_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_IP_KEY;
import static org.apache.dubbo.remoting.Constants.BIND_PORT_KEY;
import static org.apache.dubbo.remoting.Constants.CHECK_KEY;
import static org.apache.dubbo.remoting.Constants.CODEC_KEY;
import static org.apache.dubbo.remoting.Constants.CONNECTIONS_KEY;
import static org.apache.dubbo.remoting.Constants.EXCHANGER_KEY;
import static org.apache.dubbo.remoting.Constants.SERIALIZATION_KEY;
import static org.apache.dubbo.rpc.Constants.DEPRECATED_KEY;
import static org.apache.dubbo.rpc.Constants.INTERFACES;
import static org.apache.dubbo.rpc.Constants.MOCK_KEY;
import static org.apache.dubbo.rpc.Constants.TOKEN_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.EXPORT_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.REFER_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WARMUP_KEY;
import static org.apache.dubbo.rpc.cluster.Constants.WEIGHT_KEY;

/**
 * RegistryProtocol
 */
public class RegistryProtocol implements Protocol {
    public static final String[] DEFAULT_REGISTER_PROVIDER_KEYS = {
            APPLICATION_KEY, CODEC_KEY, EXCHANGER_KEY, SERIALIZATION_KEY, CLUSTER_KEY, CONNECTIONS_KEY, DEPRECATED_KEY,
            GROUP_KEY, LOADBALANCE_KEY, MOCK_KEY, PATH_KEY, TIMEOUT_KEY, TOKEN_KEY, VERSION_KEY, WARMUP_KEY,
            WEIGHT_KEY, TIMESTAMP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY
    };

    public static final String[] DEFAULT_REGISTER_CONSUMER_KEYS = {
            APPLICATION_KEY, VERSION_KEY, GROUP_KEY, DUBBO_VERSION_KEY, RELEASE_KEY
    };

    private final static Logger logger = LoggerFactory.getLogger(RegistryProtocol.class);
    private final Map<URL, NotifyListener> overrideListeners = new ConcurrentHashMap<>();
    private final Map<String, ServiceConfigurationListener> serviceConfigurationListeners = new ConcurrentHashMap<>();
    private final ProviderConfigurationListener providerConfigurationListener = new ProviderConfigurationListener();
    //To solve the problem of RMI repeated exposure port conflicts, the services that have been exposed are no longer exposed.
    //providerurl <--> exporter
    private final ConcurrentMap<String, ExporterChangeableWrapper<?>> bounds = new ConcurrentHashMap<>();
    private Cluster cluster;
    private Protocol protocol;
    private RegistryFactory registryFactory;
    private ProxyFactory proxyFactory;

    //Filter the parameters that do not need to be output in url(Starting with .)
    private static String[] getFilteredKeys(URL url) {
        Map<String, String> params = url.getParameters();
        if (CollectionUtils.isNotEmptyMap(params)) {
            return params.keySet().stream()
                    .filter(k -> k.startsWith(HIDE_KEY_PREFIX))
                    .toArray(String[]::new);
        } else {
            return new String[0];
        }
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    public void setProtocol(Protocol protocol) {
        this.protocol = protocol;
    }

    public void setRegistryFactory(RegistryFactory registryFactory) {
        this.registryFactory = registryFactory;
    }

    public void setProxyFactory(ProxyFactory proxyFactory) {
        this.proxyFactory = proxyFactory;
    }

    @Override
    public int getDefaultPort() {
        return 9090;
    }

    public Map<URL, NotifyListener> getOverrideListeners() {
        return overrideListeners;
    }

    public void register(URL registryUrl, URL registeredProviderUrl) {

        // 获取注册中心实例，如:ZookeeperRegistry
        Registry registry = registryFactory.getRegistry(registryUrl);
        // 注册服务
        registry.register(registeredProviderUrl);

        ProviderModel model = ApplicationModel.getProviderModel(registeredProviderUrl.getServiceKey());
        model.addStatedUrl(new ProviderModel.RegisterStatedURL(
                registeredProviderUrl,
                registryUrl,
                true
        ));
    }

    /**
     * 暴露服务到远程，并生成 Exporter,核心是干了两件事；1.暴露服务(即启动netty服务器) 2.向注册中心注册服务
     * 1.获得服务提供者的url，再通过override数据重新配置url，然后执行doLocalExport()进行服务暴露
     * 2.加载注册中心实现类，向注册中心注册服务
     * 3.向注册中心进行订阅 override 数据
     * 4.创建并返回 DestroyableExporter
     */
    @Override
    public <T> Exporter<T> export(final Invoker<T> originInvoker) throws RpcException {
        /**
         * 获取注册中心 URL,以zookeeper为例，得到的url如下:
         * zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-annotation-provider
         * &dubbo=2.0.2&export=dubbo://192.168.151.2:20888/org.apache.dubbo.demo.DemoService?anyhost=true
         * &application=dubbo-demo-annotation-provider&bind.ip=192.168.151.2&bind.port=20888&deprecated=false&dubbo=2.0.2
         * &dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync
         * &pid=10541&release=&side=provider&timestamp=1583895706630&pid=10541&timestamp=1583895706618
         */
        URL registryUrl = getRegistryUrl(originInvoker);
        /**
         * 服务提供者url,示例如下:
         * dubbo://192.168.151.2:20888/org.apache.dubbo.demo.DemoService?anyhost=true&application=dubbo-demo-annotation-provider
         * &bind.ip=192.168.151.2&bind.port=20888&deprecated=false&dubbo=2.0.2&dynamic=true&generic=false&interface=org.apache.dubbo.demo.DemoService
         * &methods=sayHello,sayHelloAsync&pid=10950&release=&side=provider&timestamp=1583898712310
         */
        URL providerUrl = getProviderUrl(originInvoker);

        // overrideSubscribeUrl是老版本的动态配置监听url，表示了需要监听的服务以及监听的类型（configurators， 这是老版本上的动态配置）
        // 在服务提供者url的基础上，生成一个overrideSubscribeUrl，协议为provider://，增加参数category=configurators&check=false
        final URL overrideSubscribeUrl = getSubscribedOverrideUrl(providerUrl);
        // 一个overrideSubscribeUrl对应一个OverrideListener，用来监听变化事件，监听到overrideSubscribeUrl的变化后，
        // OverrideListener就会根据变化进行相应处理，具体处理逻辑看OverrideListener的实现
        final OverrideListener overrideSubscribeListener = new OverrideListener(overrideSubscribeUrl, originInvoker);
        // 把监听器添加到集合
        overrideListeners.put(overrideSubscribeUrl, overrideSubscribeListener);

        /**
         * 在这个方法里会利用providerConfigurationListener和serviceConfigurationListener去重写providerUrl
         * providerConfigurationListener表示应用级别的动态配置监听器，providerConfigurationListener是RegistyProtocol的一个属性
         * serviceConfigurationListener表示服务级别的动态配置监听器，serviceConfigurationListener是在每暴露一个服务时就会生成一个
         * 这两个监听器都是新版本中的监听器
         * 新版本监听的zk路径是：
         * 服务： /dubbo/config/dubbo/org.apache.dubbo.demo.DemoService.configurators节点的内容
         * 应用： /dubbo/config/dubbo/dubbo-demo-provider-application.configurators节点的内容
         *
         * 注意，要喝配置中心的路径区分开来，配置中心的路径是：
         * 应用：/dubbo/config/dubbo/org.apache.dubbo.demo.DemoService/dubbo.properties节点的内容
         * 全局：/dubbo/config/dubbo/dubbo.properties节点的内容
         */
        providerUrl = overrideUrlWithConfig(providerUrl, overrideSubscribeListener);

        //1.暴露服务，其实就是启动netty服务端
        final ExporterChangeableWrapper<T> exporter = doLocalExport(originInvoker, providerUrl);

        // 根据 URL 加载 Registry 实现类，比如ZookeeperRegistry
        final Registry registry = getRegistry(originInvoker);
        // 获取将要注册到zookeeper下providers节点下的url，这里会对服务提供者url中的参数进行简化
        // 例如:dubbo://192.168.15.1:20888/org.apache.dubbo.demo.DemoService?application=dubbo-demo-annotation-provider
        // &deprecated=false&dubbo=2.0.2&timeout=6000&timestamp=1626102051421
        final URL registeredProviderUrl = getUrlToRegistry(providerUrl, registryUrl);
        // 获取 register 参数
        boolean register = providerUrl.getParameter(REGISTER_KEY, true);
        if (register) {
            // 2.向注册中心注册服务
            register(registryUrl, registeredProviderUrl);
        }

        /**
         * 针对老版本的动态配置，需要把overrideSubscribeListener绑定到overrideSubscribeUrl上去进行监听
         * 兼容老版本的配置修改，利用overrideSubscribeListener去监听旧版本的动态配置变化
         *
         * 那么新版本的providerConfigurationListener和serviceConfigurationListener是在什么时候进行订阅的呢？在这两个类构造的时候
         * 老版本监听的zk路径是：/dubbo/org.apache.dubbo.demo.DemoService/configurators/override://0.0.0.0/org.apache.dubbo
         * .demo.DemoService?category=configurators&compatible_config=true&dynamic=false&enabled=true&timeout=6000
         * 监听的是路径的内容，不是节点的内容
         */
        registry.subscribe(overrideSubscribeUrl, overrideSubscribeListener);

        // 设置注册中心url
        exporter.setRegisterUrl(registeredProviderUrl);
        // 设置override数据订阅的url
        exporter.setSubscribeUrl(overrideSubscribeUrl);

        notifyExport(exporter);

        //创建并返回DestroyableExporter
        return new DestroyableExporter<>(exporter);
    }

    private <T> void notifyExport(ExporterChangeableWrapper<T> exporter) {
        List<RegistryProtocolListener> listeners = ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                .getActivateExtension(exporter.getOriginInvoker().getUrl(), "registry.protocol.listener");
        if (CollectionUtils.isNotEmpty(listeners)) {
            for (RegistryProtocolListener listener : listeners) {
                listener.onExport(this, exporter);
            }
        }
    }

    private URL overrideUrlWithConfig(URL providerUrl, OverrideListener listener) {
        // 应用配置，providerConfigurationListener是在属性那里直接初始化好的，providerConfigurationListener会监听配置中心的应用配置信息变动
        providerUrl = providerConfigurationListener.overrideUrl(providerUrl);

        // 服务配置，new ServiceConfigurationListener的时候会初始化，ServiceConfigurationListener会监听配置中心的服务信息配置信息变动
        ServiceConfigurationListener serviceConfigurationListener = new ServiceConfigurationListener(providerUrl, listener);
        serviceConfigurationListeners.put(providerUrl.getServiceKey(), serviceConfigurationListener);

        return serviceConfigurationListener.overrideUrl(providerUrl);
    }

    // 暴露服务
    private <T> ExporterChangeableWrapper<T> doLocalExport(final Invoker<T> originInvoker, URL providerUrl) {
        String key = getCacheKey(originInvoker);

        return (ExporterChangeableWrapper<T>) bounds.computeIfAbsent(key, s -> {
            Invoker<?> invokerDelegate = new InvokerDelegate<>(originInvoker, providerUrl);
            // 调用protocol的export方法暴露服务，例如DubboProtocol
            return new ExporterChangeableWrapper<>((Exporter<T>) protocol.export(invokerDelegate), originInvoker);
        });
    }

    public <T> void reExport(Exporter<T> exporter, URL newInvokerUrl) {
        if (exporter instanceof ExporterChangeableWrapper) {
            ExporterChangeableWrapper<T> exporterWrapper = (ExporterChangeableWrapper<T>) exporter;
            Invoker<T> originInvoker = exporterWrapper.getOriginInvoker();
            reExport(originInvoker, newInvokerUrl);
        }
    }

    /**
     * 用修改过的url重新暴露服务
     *
     * @param originInvoker
     * @param newInvokerUrl
     * @param <T>
     */
    public <T> void reExport(final Invoker<T> originInvoker, URL newInvokerUrl) {
        String key = getCacheKey(originInvoker);
        ExporterChangeableWrapper<T> exporter = (ExporterChangeableWrapper<T>) bounds.get(key);
        URL registeredUrl = exporter.getRegisterUrl();

        URL registryUrl = getRegistryUrl(originInvoker);
        URL newProviderUrl = getUrlToRegistry(newInvokerUrl, registryUrl);

        //修改ExporterChangeableWrapper中持有的DubboExporter
        Invoker<T> invokerDelegate = new InvokerDelegate<T>(originInvoker, newInvokerUrl);
        exporter.setExporter(protocol.export(invokerDelegate));

        //修改zookeeper上保存的服务信息
        if (!newProviderUrl.equals(registeredUrl)) {
            if (getProviderUrl(originInvoker).getParameter(REGISTER_KEY, true)) {
                Registry registry = getRegistry(originInvoker);
                logger.info("Try to unregister old url: " + registeredUrl);
                registry.unregister(registeredUrl);

                logger.info("Try to register new url: " + newProviderUrl);
                registry.register(newProviderUrl);
            }

            ProviderModel.RegisterStatedURL statedUrl = getStatedUrl(registryUrl, newProviderUrl);
            statedUrl.setProviderUrl(newProviderUrl);
            exporter.setRegisterUrl(newProviderUrl);
        }
    }

    private ProviderModel.RegisterStatedURL getStatedUrl(URL registryUrl, URL providerUrl) {
        ProviderModel providerModel = ApplicationModel.getServiceRepository()
                .lookupExportedService(providerUrl.getServiceKey());

        List<ProviderModel.RegisterStatedURL> statedUrls = providerModel.getStatedUrl();
        return statedUrls.stream()
                .filter(u -> u.getRegistryUrl().equals(registryUrl)
                        && u.getProviderUrl().getProtocol().equals(providerUrl.getProtocol()))
                .findFirst().orElseThrow(() -> new IllegalStateException("There should have at least one registered url."));
    }

    /**
     * Get an instance of registry based on the address of invoker
     *
     * @param originInvoker
     * @return
     */
    protected Registry getRegistry(final Invoker<?> originInvoker) {
        URL registryUrl = getRegistryUrl(originInvoker);
        return registryFactory.getRegistry(registryUrl);
    }

    protected URL getRegistryUrl(Invoker<?> originInvoker) {
        URL registryUrl = originInvoker.getUrl();
        if (REGISTRY_PROTOCOL.equals(registryUrl.getProtocol())) {
            String protocol = registryUrl.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY);
            registryUrl = registryUrl.setProtocol(protocol).removeParameter(REGISTRY_KEY);
        }
        return registryUrl;
    }

    protected URL getRegistryUrl(URL url) {
        return URLBuilder.from(url)
                .setProtocol(url.getParameter(REGISTRY_KEY, DEFAULT_REGISTRY))
                .removeParameter(REGISTRY_KEY)
                .build();
    }


    /**
     * Return the url that is registered to the registry and filter the url parameter once
     *
     * @param providerUrl
     * @return url to registry.
     */
    private URL getUrlToRegistry(final URL providerUrl, final URL registryUrl) {
        //The address you see at the registry
        if (!registryUrl.getParameter(SIMPLIFIED_KEY, false)) {
            return providerUrl.removeParameters(getFilteredKeys(providerUrl)).removeParameters(
                    MONITOR_KEY, BIND_IP_KEY, BIND_PORT_KEY, QOS_ENABLE, QOS_HOST, QOS_PORT, ACCEPT_FOREIGN_IP, VALIDATION_KEY,
                    INTERFACES);
        } else {
            String extraKeys = registryUrl.getParameter(EXTRA_KEYS_KEY, "");
            // if path is not the same as interface name then we should keep INTERFACE_KEY,
            // otherwise, the registry structure of zookeeper would be '/dubbo/path/providers',
            // but what we expect is '/dubbo/interface/providers'
            if (!providerUrl.getPath().equals(providerUrl.getParameter(INTERFACE_KEY))) {
                if (StringUtils.isNotEmpty(extraKeys)) {
                    extraKeys += ",";
                }
                extraKeys += INTERFACE_KEY;
            }
            String[] paramsToRegistry = getParamsToRegistry(DEFAULT_REGISTER_PROVIDER_KEYS
                    , COMMA_SPLIT_PATTERN.split(extraKeys));
            return URL.valueOf(providerUrl, paramsToRegistry, providerUrl.getParameter(METHODS_KEY, (String[]) null));
        }

    }

    private URL getSubscribedOverrideUrl(URL registeredProviderUrl) {
        return registeredProviderUrl.setProtocol(PROVIDER_PROTOCOL)
                .addParameters(CATEGORY_KEY, CONFIGURATORS_CATEGORY, CHECK_KEY, String.valueOf(false));
    }

    /**
     * Get the address of the providerUrl through the url of the invoker
     *
     * @param originInvoker
     * @return
     */
    private URL getProviderUrl(final Invoker<?> originInvoker) {
        String export = originInvoker.getUrl().getParameterAndDecoded(EXPORT_KEY);
        if (export == null || export.length() == 0) {
            throw new IllegalArgumentException("The registry export url is null! registry: " + originInvoker.getUrl());
        }
        return URL.valueOf(export);
    }

    /**
     * Get the key cached in bounds by invoker
     *
     * @param originInvoker
     * @return
     */
    private String getCacheKey(final Invoker<?> originInvoker) {
        URL providerUrl = getProviderUrl(originInvoker);
        String key = providerUrl.removeParameters("dynamic", "enabled").toFullString();
        return key;
    }

    /**
     * 引用服务，生成Invoke
     */
    @Override
    public <T> Invoker<T> refer(Class<T> type, URL url) throws RpcException {
        /**
         * 获取注册中心url:如:
         * zookeeper://127.0.0.1:2181/org.apache.dubbo.registry.RegistryService?application=dubbo-demo-annotation-consumer
         * &dubbo=2.0.2&pid=16490&refer=application=dubbo-demo-annotation-consumer&dubbo=2.0.2&init=false
         * &interface=org.apache.dubbo.demo.DemoService&methods=sayHello,sayHelloAsync&pid=16490&register.ip=192.168.1.8
         * &side=consumer&sticky=false&timestamp=1583995362301&timestamp=1583995363363
         */
        url = getRegistryUrl(url);
        // 获取注册中心示例，如:ZookeeperRegistry
        Registry registry = registryFactory.getRegistry(url);
        // 如果是注册中心服务，则返回注册中心服务的invoker
        if (RegistryService.class.equals(type)) {
            return proxyFactory.getInvoker((T) registry, type, url);
        }

        // group="a,b" or group="*"
        // 将 url 查询字符串转为 Map
        Map<String, String> qs = StringUtils.parseQueryString(url.getParameterAndDecoded(REFER_KEY));
        String group = qs.get(GROUP_KEY);
        if (group != null && group.length() > 0) {
            if ((COMMA_SPLIT_PATTERN.split(group)).length > 1 || "*".equals(group)) {
                // 如果有多个组，或者组配置为*，则使用MergeableCluster，并调用 doRefer 继续执行服务引用逻辑
                return doRefer(getMergeableCluster(), registry, type, url);
            }
        }
        // 只有一个组或者没有组配置，则直接执行doRefer
        return doRefer(cluster, registry, type, url);
    }

    private Cluster getMergeableCluster() {
        return ExtensionLoader.getExtensionLoader(Cluster.class).getExtension("mergeable");
    }

    private <T> Invoker<T> doRefer(Cluster cluster, Registry registry, Class<T> type, URL url) {
        // 创建 RegistryDirectory 实例
        RegistryDirectory<T> directory = new RegistryDirectory<T>(type, url);
        // 设置注册中心
        directory.setRegistry(registry);
        // 设置协议
        directory.setProtocol(protocol);
        // 所有属性放到map中
        Map<String, String> parameters = new HashMap<>(directory.getConsumerUrl().getParameters());
        // 生成服务消费者链接
        URL subscribeUrl = new URL(CONSUMER_PROTOCOL, parameters.remove(REGISTER_IP_KEY), 0, type.getName(), parameters);
        if (directory.isShouldRegister()) {
            directory.setRegisteredConsumerUrl(subscribeUrl);
            /**
             * 注册服务消费者，注册到consumers目录下：
             * consumer://172.19.7.245/org.apache.dubbo.demo.DemoService?application=dubbo-demo-annotation-consumer
             * &category=consumers&check=false&dubbo=2.0.2&init=false&interface=org.apache.dubbo.demo.DemoService
             * &metadata-type=remote&methods=sayHello,sayHelloAsync&pid=44961&side=consumer&sticky=false&timestamp=1626226005453
             */
            registry.register(directory.getRegisteredConsumerUrl());
        }
        /**
         * 构造路由链,路由链会在引入服务时按路由条件进行过滤
         * 路由链是动态服务目录中的一个属性，通过路由链可以过滤某些服务提供者
         */
        directory.buildRouterChain(subscribeUrl);
        /**
         * 服务目录需要订阅的几个路径
         * 当前应用所对应的动态配置目录：/dubbo/config/dubbo/dubbo-demo-consumer-application.configurators
         * 当前所引入的服务的动态配置目录：/dubbo/config/dubbo/org.apache.dubbo.demo.DemoService:1.1.1:g1.configurators
         * 当前所引入的服务的提供者目录：/dubbo/org.apache.dubbo.demo.DemoService/providers
         * 当前所引入的服务的老版本动态配置目录：/dubbo/org.apache.dubbo.demo.DemoService/configurators
         * 当前所引入的服务的老版本路由器目录：/dubbo/org.apache.dubbo.demo.DemoService/routers
         */
        directory.subscribe(toSubscribeUrl(subscribeUrl));

        // 一个注册中心可能有多个服务提供者，因此这里需要将多个服务提供者合并为一个，生成一个invoker，通常为MockClusterInvoker
        Invoker<T> invoker = cluster.join(directory);
        List<RegistryProtocolListener> listeners = findRegistryProtocolListeners(url);
        if (CollectionUtils.isEmpty(listeners)) {
            return invoker;
        }

        RegistryInvokerWrapper<T> registryInvokerWrapper = new RegistryInvokerWrapper<>(directory, cluster, invoker, subscribeUrl);
        for (RegistryProtocolListener listener : listeners) {
            listener.onRefer(this, registryInvokerWrapper);
        }
        return registryInvokerWrapper;
    }

    public <T> void reRefer(Invoker<T> invoker, URL newSubscribeUrl) {
        if (!(invoker instanceof RegistryInvokerWrapper)) {
            return;
        }

        RegistryInvokerWrapper<T> invokerWrapper = (RegistryInvokerWrapper<T>) invoker;
        URL oldSubscribeUrl = invokerWrapper.getUrl();
        RegistryDirectory<T> directory = invokerWrapper.getDirectory();
        Registry registry = directory.getRegistry();
        registry.unregister(directory.getRegisteredConsumerUrl());
        directory.unSubscribe(toSubscribeUrl(oldSubscribeUrl));

        directory.setRegisteredConsumerUrl(newSubscribeUrl);
        registry.register(directory.getRegisteredConsumerUrl());
        directory.buildRouterChain(newSubscribeUrl);
        directory.subscribe(toSubscribeUrl(newSubscribeUrl));

        invokerWrapper.setInvoker(invokerWrapper.getCluster().join(directory));
        invokerWrapper.setUrl(newSubscribeUrl);
    }

    private static URL toSubscribeUrl(URL url) {
        return url.addParameter(CATEGORY_KEY, PROVIDERS_CATEGORY + "," + CONFIGURATORS_CATEGORY + "," + ROUTERS_CATEGORY);
    }

    private List<RegistryProtocolListener> findRegistryProtocolListeners(URL url) {
        return ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                .getActivateExtension(url, "registry.protocol.listener");
    }

    // available to test
    public String[] getParamsToRegistry(String[] defaultKeys, String[] additionalParameterKeys) {
        int additionalLen = additionalParameterKeys.length;
        String[] registryParams = new String[defaultKeys.length + additionalLen];
        System.arraycopy(defaultKeys, 0, registryParams, 0, defaultKeys.length);
        System.arraycopy(additionalParameterKeys, 0, registryParams, defaultKeys.length, additionalLen);
        return registryParams;
    }

    @Override
    public void destroy() {
        List<RegistryProtocolListener> listeners = ExtensionLoader.getExtensionLoader(RegistryProtocolListener.class)
                .getLoadedExtensionInstances();
        if (CollectionUtils.isNotEmpty(listeners)) {
            for (RegistryProtocolListener listener : listeners) {
                listener.onDestroy();
            }
        }

        List<Exporter<?>> exporters = new ArrayList<Exporter<?>>(bounds.values());
        for (Exporter<?> exporter : exporters) {
            exporter.unexport();
        }
        bounds.clear();

        ExtensionLoader.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
                .removeListener(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX, providerConfigurationListener);
    }

    @Override
    public List<ProtocolServer> getServers() {
        return protocol.getServers();
    }

    //Merge the urls of configurators
    private static URL getConfigedInvokerUrl(List<Configurator> configurators, URL url) {
        if (configurators != null && configurators.size() > 0) {
            for (Configurator configurator : configurators) {
                url = configurator.configure(url);
            }
        }
        return url;
    }

    public static class InvokerDelegate<T> extends InvokerWrapper<T> {
        private final Invoker<T> invoker;

        /**
         * @param invoker
         * @param url     invoker.getUrl return this value
         */
        public InvokerDelegate(Invoker<T> invoker, URL url) {
            super(invoker, url);
            this.invoker = invoker;
        }

        public Invoker<T> getInvoker() {
            if (invoker instanceof InvokerDelegate) {
                return ((InvokerDelegate<T>) invoker).getInvoker();
            } else {
                return invoker;
            }
        }
    }

    private static class DestroyableExporter<T> implements Exporter<T> {

        private Exporter<T> exporter;

        public DestroyableExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        @Override
        public void unexport() {
            exporter.unexport();
        }
    }

    /**
     * Reexport: the exporter destroy problem in protocol
     * 1.Ensure that the exporter returned by registryprotocol can be normal destroyed
     * 2.No need to re-register to the registry after notify
     * 3.The invoker passed by the export method , would better to be the invoker of exporter
     */
    private class OverrideListener implements NotifyListener {
        private final URL subscribeUrl;
        private final Invoker originInvoker;


        private List<Configurator> configurators;

        public OverrideListener(URL subscribeUrl, Invoker originalInvoker) {
            this.subscribeUrl = subscribeUrl;
            this.originInvoker = originalInvoker;
        }

        /**
         * @param urls The list of registered information, is always not empty, The meaning is the same as the
         *             return value of {@link org.apache.dubbo.registry.RegistryService#lookup(URL)}.
         */
        @Override
        public synchronized void notify(List<URL> urls) {
            logger.debug("original override urls: " + urls);

            List<URL> matchedUrls = getMatchedUrls(urls, subscribeUrl.addParameter(CATEGORY_KEY, CONFIGURATORS_CATEGORY));
            logger.debug("subscribe url: " + subscribeUrl + ", override urls: " + matchedUrls);

            // No matching results
            if (matchedUrls.isEmpty()) {
                return;
            }

            this.configurators =
                Configurator.toConfigurators(classifyUrls(matchedUrls, UrlUtils::isConfigurator)).orElse(configurators);

            doOverrideIfNecessary();
        }

        public synchronized void doOverrideIfNecessary() {
            final Invoker<?> invoker;
            if (originInvoker instanceof InvokerDelegate) {
                invoker = ((InvokerDelegate<?>) originInvoker).getInvoker();
            } else {
                invoker = originInvoker;
            }
            //The origin invoker
            URL originUrl = RegistryProtocol.this.getProviderUrl(invoker);
            String key = getCacheKey(originInvoker);
            ExporterChangeableWrapper<?> exporter = bounds.get(key);
            if (exporter == null) {
                logger.warn(new IllegalStateException("error state, exporter should not be null"));
                return;
            }
            //The current, may have been merged many times
            URL currentUrl = exporter.getInvoker().getUrl();
            //用override配置覆盖currentUrl
            URL newUrl = getConfigedInvokerUrl(configurators, currentUrl);

            newUrl = getConfigedInvokerUrl(providerConfigurationListener.getConfigurators(), newUrl);
            newUrl = getConfigedInvokerUrl(serviceConfigurationListeners.get(originUrl.getServiceKey()).getConfigurators(), newUrl);
            if (!currentUrl.equals(newUrl)) {
                //重新暴露服务
                RegistryProtocol.this.reExport(originInvoker, newUrl);
                logger.info("exported provider url changed, origin url: " + originUrl +
                        ", old export url: " + currentUrl + ", new export url: " + newUrl);
            }
        }

        private List<URL> getMatchedUrls(List<URL> configuratorUrls, URL currentSubscribe) {
            List<URL> result = new ArrayList<URL>();
            for (URL url : configuratorUrls) {
                URL overrideUrl = url;
                // Compatible with the old version
                if (url.getParameter(CATEGORY_KEY) == null && OVERRIDE_PROTOCOL.equals(url.getProtocol())) {
                    overrideUrl = url.addParameter(CATEGORY_KEY, CONFIGURATORS_CATEGORY);
                }

                // Check whether url is to be applied to the current service
                if (UrlUtils.isMatch(currentSubscribe, overrideUrl)) {
                    result.add(url);
                }
            }
            return result;
        }
    }

    private class ServiceConfigurationListener extends AbstractConfiguratorListener {
        private URL providerUrl;
        private OverrideListener notifyListener;

        public ServiceConfigurationListener(URL providerUrl, OverrideListener notifyListener) {
            this.providerUrl = providerUrl;
            this.notifyListener = notifyListener;
            this.initWith(DynamicConfiguration.getRuleKey(providerUrl) + CONFIGURATORS_SUFFIX);
        }

        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfigedInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
            notifyListener.doOverrideIfNecessary();
        }
    }

    private class ProviderConfigurationListener extends AbstractConfiguratorListener {

        public ProviderConfigurationListener() {
            this.initWith(ApplicationModel.getApplication() + CONFIGURATORS_SUFFIX);
        }

        /**
         * Get existing configuration rule and override provider url before exporting.
         *
         * @param providerUrl
         * @param <T>
         * @return
         */
        private <T> URL overrideUrl(URL providerUrl) {
            return RegistryProtocol.getConfigedInvokerUrl(configurators, providerUrl);
        }

        @Override
        protected void notifyOverrides() {
            overrideListeners.values().forEach(listener -> ((OverrideListener) listener).doOverrideIfNecessary());
        }
    }

    /**
     * exporter proxy, establish the corresponding relationship between the returned exporter and the exporter
     * exported by the protocol, and can modify the relationship at the time of override.
     *
     * @param <T>
     */
    private class ExporterChangeableWrapper<T> implements Exporter<T> {

        private final ExecutorService executor = newSingleThreadExecutor(new NamedThreadFactory("Exporter-Unexport", true));

        private final Invoker<T> originInvoker;
        private Exporter<T> exporter;
        private URL subscribeUrl;
        private URL registerUrl;

        public ExporterChangeableWrapper(Exporter<T> exporter, Invoker<T> originInvoker) {
            this.exporter = exporter;
            this.originInvoker = originInvoker;
        }

        public Invoker<T> getOriginInvoker() {
            return originInvoker;
        }

        @Override
        public Invoker<T> getInvoker() {
            return exporter.getInvoker();
        }

        public void setExporter(Exporter<T> exporter) {
            this.exporter = exporter;
        }

        @Override
        public void unexport() {
            String key = getCacheKey(this.originInvoker);
            bounds.remove(key);

            Registry registry = RegistryProtocol.this.getRegistry(originInvoker);
            try {
                registry.unregister(registerUrl);
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }
            try {
                NotifyListener listener = RegistryProtocol.this.overrideListeners.remove(subscribeUrl);
                registry.unsubscribe(subscribeUrl, listener);
                ExtensionLoader.getExtensionLoader(GovernanceRuleRepository.class).getDefaultExtension()
                        .removeListener(subscribeUrl.getServiceKey() + CONFIGURATORS_SUFFIX,
                                serviceConfigurationListeners.get(subscribeUrl.getServiceKey()));
            } catch (Throwable t) {
                logger.warn(t.getMessage(), t);
            }

            executor.submit(() -> {
                try {
                    int timeout = ConfigurationUtils.getServerShutdownTimeout();
                    if (timeout > 0) {
                        logger.info("Waiting " + timeout + "ms for registry to notify all consumers before unexport. " +
                                "Usually, this is called when you use dubbo API");
                        Thread.sleep(timeout);
                    }
                    exporter.unexport();
                } catch (Throwable t) {
                    logger.warn(t.getMessage(), t);
                }
            });
        }

        public void setSubscribeUrl(URL subscribeUrl) {
            this.subscribeUrl = subscribeUrl;
        }

        public void setRegisterUrl(URL registerUrl) {
            this.registerUrl = registerUrl;
        }

        public URL getRegisterUrl() {
            return registerUrl;
        }
    }

    // for unit test
    private static RegistryProtocol INSTANCE;

    // for unit test
    public RegistryProtocol() {
        INSTANCE = this;
    }

    // for unit test
    public static RegistryProtocol getRegistryProtocol() {
        if (INSTANCE == null) {
            ExtensionLoader.getExtensionLoader(Protocol.class).getExtension(REGISTRY_PROTOCOL); // load
        }
        return INSTANCE;
    }
}
