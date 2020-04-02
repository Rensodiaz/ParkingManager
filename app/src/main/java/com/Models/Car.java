package com.Models;

import com.helpers.AppDataBase;
import com.reactiveandroid.Model;
import com.reactiveandroid.annotation.Column;
import com.reactiveandroid.annotation.PrimaryKey;
import com.reactiveandroid.annotation.Table;

@Table(name = "Cars", database = AppDataBase.class)
public class Car extends Model {

    @PrimaryKey
    private Long id;
    @Column(name = "plate")
    private String plate;
    @Column(name = "color")
    private String color;
    @Column(name = "make")
    private String make;
    @Column(name = "barcode")
    private String barcode;
    @Column(name = "date_in")
    private String in;
    @Column(name = "date_out")
    private String out;
    //This is to know if the ticket have been Paid, Not_paid or Void
    @Column(name = "status")
    private String status;
    @Column(name = "total")
    private int total;

    //ReActiveAndroid requires empty constructor
    public Car(){};

    public Car(String plate, String color, String make, String barcode, String in, String status){
        this.plate = plate;
        this.color = color;
        this.make = make;
        this.barcode = barcode;
        this.in = in;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlate() {
        return plate;
    }

    public void setPlate(String plate) {
        this.plate = plate;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getIn() {
        return in;
    }

    public void setIn(String in) {
        this.in = in;
    }

    public String getOut() {
        return out;
    }

    public void setOut(String out) {
        this.out = out;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }
}
