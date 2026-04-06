package org.example.scan.proxy;

public class SecondProxyBean extends OriginBean {
    final OriginBean target;

    public SecondProxyBean(OriginBean target) {
        this.target = target;
    }

    @Override
    public void setVersion(String version) {
        this.target.setVersion(version);
    }

    @Override
    public String getName() {
        return this.target.getName();
    }

    @Override
    public String getVersion() {
        return this.target.getVersion();
    }
}
