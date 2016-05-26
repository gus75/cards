package ru.rik.cardsnew.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor

@Entity
public class Bank {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    @Column //(nullable = false)
	String location;
    
    @Column //(nullable = false,  unique=true)
    String ip;
    
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "bank")
    Set<Card> cards = new HashSet<>();

}
