package ru.rik.cardsnew.domain;

import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.springframework.cache.annotation.Cacheable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Builder;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@ToString(exclude={"cards","channels"})
//@Table(uniqueConstraints = {@UniqueConstraint(columnNames = "name")})
@Cacheable
@org.hibernate.annotations.Cache( usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name="_GRP")
public class Grp implements MyEntity {
	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
	@Getter @Setter
    private long id;
	
	@Version
	@Getter @Setter
	protected long version;
	 
	@Getter @Setter
	@Column (nullable = false,  unique=true)
	String name;
	
	@Column
	@Getter @Setter
	private Oper oper;
	
	@Getter @Setter
    @OneToMany(mappedBy = "group")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    Set<Card> cards;

	@Getter @Setter
    @OneToMany(mappedBy = "group")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    Set<Channel> channels;
	
	@Getter @Setter
	@Column
	private String descr;
}
