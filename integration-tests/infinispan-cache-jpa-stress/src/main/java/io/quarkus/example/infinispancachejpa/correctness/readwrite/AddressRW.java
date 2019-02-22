/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package io.quarkus.example.infinispancachejpa.correctness.readwrite;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.persistence.*;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

@Entity
public final class AddressRW {

    @Id
    @GeneratedValue
    private int id;
    private int streetNumber;
    private String streetName;
    private String cityName;
    private String countryName;
    private String zipCode;
    @OneToMany
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private Set<MemberRW> inhabitants;
    @Version
    private int version;

    public AddressRW(int streetNumber, String streetName, String cityName, String countryName) {
        this.streetNumber = streetNumber;
        this.streetName = streetName;
        this.cityName = cityName;
        this.countryName = countryName;
        this.zipCode = null;
        this.inhabitants = new HashSet<MemberRW>();
        this.id = 0;
        this.version = 0;
    }

    protected AddressRW() {
        this.streetNumber = 0;
        this.streetName = null;
        this.cityName = null;
        this.countryName = null;
        this.zipCode = null;
        this.inhabitants = new HashSet<MemberRW>();
        this.id = 0;
        this.version = 0;
    }

    public int getStreetNumber() {
        return streetNumber;
    }

    public String getStreetName() {
        return streetName;
    }

    public String getCityName() {
        return cityName;
    }

    public String getCountryName() {
        return countryName;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Set<MemberRW> getInhabitants() {
        return inhabitants;
    }

    public boolean addInhabitant(MemberRW inhabitant) {
        boolean done = false;
        if (inhabitants.add(inhabitant)) {
            inhabitant.setAddress(this);
            done = true;
        }
        return done;
    }

    public boolean remInhabitant(MemberRW inhabitant) {
        boolean done = false;
        if (inhabitants.remove(inhabitant)) {
            inhabitant.setAddress(null);
            done = true;
        }
        return done;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getVersion() {
        return version;
    }

    protected void removeAllInhabitants() {
        // inhabitants relation is not CASCADED, we must delete the relation on other side by ourselves
        for (Iterator<MemberRW> iterator = inhabitants.iterator(); iterator.hasNext();) {
            MemberRW p = iterator.next();
            p.setAddress(null);
        }
    }

    protected void setStreetNumber(int streetNumber) {
        this.streetNumber = streetNumber;
    }

    protected void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    protected void setCityName(String cityName) {
        this.cityName = cityName;
    }

    protected void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    protected void setInhabitants(Set<MemberRW> inhabitants) {
        if (inhabitants == null) {
            this.inhabitants = new HashSet<MemberRW>();
        } else {
            this.inhabitants = inhabitants;
        }
    }

    protected void setVersion(Integer version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AddressRW address = (AddressRW) o;

        // inhabitants must not be in the comparison since we would end up in infinite recursion
        if (id != address.id)
            return false;
        if (streetNumber != address.streetNumber)
            return false;
        if (version != address.version)
            return false;
        if (cityName != null ? !cityName.equals(address.cityName) : address.cityName != null)
            return false;
        if (countryName != null ? !countryName.equals(address.countryName) : address.countryName != null)
            return false;
        if (streetName != null ? !streetName.equals(address.streetName) : address.streetName != null)
            return false;
        if (zipCode != null ? !zipCode.equals(address.zipCode) : address.zipCode != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = streetNumber;
        result = 31 * result + (streetName != null ? streetName.hashCode() : 0);
        result = 31 * result + (cityName != null ? cityName.hashCode() : 0);
        result = 31 * result + (countryName != null ? countryName.hashCode() : 0);
        result = 31 * result + (zipCode != null ? zipCode.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + version;
        return result;
    }

    @Override
    public String toString() {
        return "Address{" +
                "cityName='" + cityName + '\'' +
                ", streetNumber=" + streetNumber +
                ", streetName='" + streetName + '\'' +
                ", countryName='" + countryName + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", inhabitants=" + inhabitants +
                ", id=" + id +
                ", version=" + version +
                '}';
    }

}
