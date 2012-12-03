package shaz.rmc.core.domain;

import java.io.Serializable;

public class VehicleType implements Serializable {

    private final String name;
    
    public VehicleType(final String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        VehicleType other = (VehicleType) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() +"[name="+name+"]";
    }
}
