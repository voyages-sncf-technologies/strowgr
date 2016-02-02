package com.vsct.dt.haas.state;

/**
 * IMMUTABLE HAPROXY
 */
public class Haproxy {

    private final String ipMaster;
    private final String ipSlave;

    public Haproxy(String ipMaster, String ipSlave) {
        this.ipMaster = ipMaster;
        this.ipSlave = ipSlave;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Haproxy haproxy = (Haproxy) o;

        if (ipMaster != null ? !ipMaster.equals(haproxy.ipMaster) : haproxy.ipMaster != null) return false;
        if (ipSlave != null ? !ipSlave.equals(haproxy.ipSlave) : haproxy.ipSlave != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = ipMaster != null ? ipMaster.hashCode() : 0;
        result = 31 * result + (ipSlave != null ? ipSlave.hashCode() : 0);
        return result;
    }
}
