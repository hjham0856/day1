package com.sk.skala.day1.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class Order {
    @Id
    @GeneratedValue

    Long id;
    String Sender;
    String Reciever;
    String productName;

    public Order(){}

    public Order(Long id,String Sender,String Reciever,String productName){
        this.id=id; 
        this.Sender=Sender; 
        this.Reciever=Reciever; 
        this.productName=productName;
    }


}