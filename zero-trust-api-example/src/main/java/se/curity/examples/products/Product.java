package se.curity.examples.products;

import javax.json.JsonObject;
import javax.json.JsonValue;
import java.util.Collection;
import java.util.Locale;

public class Product {

    private final String id;
    private final String name;
    private final String description;
    private final boolean isExclusive;
    private final Collection<String> authorizedCountries ;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDetails() {
        return description;
    }

    public boolean IsExclusive() {
        return isExclusive;
    }

    public Collection<String> getAuthorizedCountries() {
        return authorizedCountries;
    }

    public void addAuthorizedCountry(String countryCode) {
        if (Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2).contains(countryCode)) {
            if (!authorizedCountries.contains(countryCode)) {
                this.authorizedCountries.add(countryCode);
            }
        }
    }

    public void removeAuthorizedCountry(String countryCode) {
        this.authorizedCountries.remove(countryCode);
    }

    public Product(String id, String name, String description, Collection<String> authorizedCountries) {
        this(id, name, description, authorizedCountries, false);
    }

    public Product(String id, String name, String description, Collection<String> authorizedCountries , boolean isExclusive) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.authorizedCountries = authorizedCountries;
        this.isExclusive = isExclusive;
    }
}
