package org.apache.dubbo.rpc.cluster;

import org.apache.dubbo.common.extension.ExtensionLoader;

//Cluster类的自适应拓展类
public class Cluster$Adaptive implements Cluster {

    public org.apache.dubbo.rpc.Invoker join(Directory arg0) throws org.apache.dubbo.rpc.RpcException {
        if (arg0 == null) throw new IllegalArgumentException("org.apache.dubbo.rpc.cluster.Directory argument == null");
        if (arg0.getUrl() == null)
            throw new IllegalArgumentException("org.apache.dubbo.rpc.cluster.Directory argument getUrl() == null");

        org.apache.dubbo.common.URL url = arg0.getUrl();
        String extName = url.getParameter("cluster", "failover");
        if (extName == null)
            throw new IllegalStateException("Failed to get extension (org.apache.dubbo.rpc.cluster.Cluster) name from url (" + url.toString() + ") use keys([cluster])");
        Cluster extension = (Cluster) ExtensionLoader.getExtensionLoader(Cluster.class).getExtension(extName);
        return extension.join(arg0);
    }

}
