package com.fooddelivery.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

public class Address implements Serializable {

    @NotBlank(message = "Street is required")
    @Size(max = 255)
    private String street;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @Size(max = 50)
    private String apartment; // optional

    private Double lat;
    private Double lng;

    @Size(max = 500)
    private String comment; // optional

    // getters and setters
    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getApartment() { return apartment; }
    public void setApartment(String apartment) { this.apartment = apartment; }

    public Double getLat() { return lat; }
    public void setLat(Double lat) { this.lat = lat; }

    public Double getLng() { return lng; }
    public void setLng(Double lng) { this.lng = lng; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}