package ru.rik.cardsnew.domain;

import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Builder;

@Entity @Table(name="_BALANCE")
@NoArgsConstructor  @AllArgsConstructor @Builder 
@EqualsAndHashCode(exclude = {"card"})
@NamedQueries({ 
	@NamedQuery(name = "findAllBalance", query = "SELECT u FROM Balance u "),
	@NamedQuery(name = "findAllLastBalance", 
	query = "SELECT b FROM Balance b WHERE b.date = "
			+ "(SELECT MAX(b1.date) FROM Balance b1 WHERE b1.card = b.card)")
	}
)
public class Balance {
	@Id  @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Getter @Setter private long id;
	@Getter @Setter private Date date;
	@Getter @Setter private String decodedmsg;
	@Getter @Setter private Float balance;
	@ManyToOne(optional = false)
	@Getter @Setter private Card card;
	@Getter @Setter private boolean payment;
	@Getter @Setter private boolean smsNeeded;
	
	@Override
	public String toString() {
		return "Balance [id=" + id + ", date=" + date + ", decodedmsg=" + decodedmsg + ", balance=" + balance
				+ ", card=" + card.getId() + ", payment=" + payment + ", smsNeeded=" + smsNeeded + "]";
	}
	
}