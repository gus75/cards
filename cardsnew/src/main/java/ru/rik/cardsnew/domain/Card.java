package ru.rik.cardsnew.domain;


import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.Random;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.cache.annotation.Cacheable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Builder;
import ru.rik.cardsnew.config.Settings;
import ru.rik.cardsnew.db.BankRepo;
import ru.rik.cardsnew.db.CardRepo;

@NamedQueries({ 
	@NamedQuery(name = "findAllCardsInGrp", query = "SELECT c FROM Card c WHERE c.group = :g and c.blocked = false"), 
	@NamedQuery(name = "findActiveCardsInGrp", query = "SELECT c FROM Card c "
			+ "WHERE c.group = :g and c.active = true and c.blocked = false"),
	@NamedQuery(name = "findCardByPlace", query = "SELECT c FROM Card c "
			+ "WHERE c.place = :place and c.bank = :bank and c.blocked = false")
	}
)
@NoArgsConstructor @AllArgsConstructor @Builder 
@EqualsAndHashCode(exclude = { "group", "bank"},callSuper = false)
@ToString(exclude = {"group", "bank"})
@Entity @Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="_CARD", uniqueConstraints=@UniqueConstraint(columnNames={"bank_id", "place"}))
public class Card implements MyEntity {
    @Id @Getter @Setter
    @Column(name="id")
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;
    
    @Version @Getter @Setter
    protected long version;
    
    @Column(unique=true, nullable = false)
    @NotNull(message = "{error.card.name.null}")
	@NotEmpty(message = "{error.card.name.empty}")
	@Size(min = 3, max = 20, message = "{error.card.name.size}")
//    @NaturalId(mutable = true)
    @Getter @Setter
	private String name;
    
    @Column(unique=true)
    @Size(min = 10, max = 10, message = "number has to be equal to 10 digits")
    @Getter @Setter
	private String number;
	
    @Getter @Setter
	private Place place;
    
	@NotNull(message = "{error.card.im.null}")
	@NotEmpty(message = "{error.card.im.empty}")
	@Size(min = 3, max = 20, message = "sernumber has to be more than 3 and less 10 digits")
    @Column(unique=true, nullable = false)
	@Getter @Setter private String sernumber;
    
    @ManyToOne(optional = false) //Default- Eager
    @Getter @Setter private Grp group;
    
    @ManyToOne
    @Getter @Setter	private Bank bank;
    
    @Getter @Setter	private long channelId;
	 
    @Getter @Setter private boolean active;
    
    @Getter @Setter private int dlimit;
    
    @Getter @Setter private int mlimit;
    
    @Getter @Setter private String descr;
    
    @Getter @Setter private Date activation;
        
    @Getter @Setter private boolean blocked;

    @Getter @Setter private Date blockdate;
    
    @ManyToOne
    @NotNull(message = "Card's limit can not be null")
    @Getter @Setter private Limit limit;

    @Getter @Setter private boolean offnetPos;
    
    @Getter @Setter private boolean mskSeparate;
    
    @Transient
    @Getter @Setter private CardStat stat;
    
    public CardStat getStat (CardRepo repo) {
    	return repo.findStateById(id); 
    }
    
    /** tries to make CardStat not free. If fails, throws ConcurrentModificationException*/
    public void engage(CardStat st) {
		if (!st.setFree(true, false)) 
			throw new ConcurrentModificationException("Card " + getName() + " is already engaged");
	}
    
    
    public void refreshDayLimit( ) {
    	Random rnd = new Random();
    	dlimit = limit.getF() + rnd.nextInt(limit.getT() - limit.getF());
    }
    
    
    public boolean isEligibleToInstall(CardRepo cards, BankRepo banks) {
    	CardStat st = getStat(cards);
    	return isActive() && !isBlocked()
    			&& getBank().getStat(banks).isAvailable() 
				&& st.isFree() 
				&& (st.getBalance() > Settings.MIN_AVAILABLE_BALANCE || st.isRefilled())
				&& st.getMinRemains() > 1;
    }
    
   
	public String toStringAll() {
		return toString() 
				+ " group: " + ( group != null ? group.getId() : "none") 
				+ " bank: "  + (bank != null ? bank.getName() : "none"); 
//				+ " ch: "    + (channel != null ? channel.getName() : "none");
	}
	

}
