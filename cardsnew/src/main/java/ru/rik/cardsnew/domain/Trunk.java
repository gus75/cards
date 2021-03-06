package ru.rik.cardsnew.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.NaturalId;
import org.springframework.cache.annotation.Cacheable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import ru.rik.cardsnew.config.AppInitializer;
import ru.rik.cardsnew.db.TrunkRepo;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = { "channels" })
@EqualsAndHashCode(exclude = { "channels" })
@Entity 
@Cacheable
@org.hibernate.annotations.Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Table(name = "_TRUNK")
public class Trunk implements MyEntity {

	@Id	@Column(name = "id") @GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Version @Getter @Setter
	protected long version;

	@Column(nullable = false, unique = true)
	@NaturalId(mutable = true)
	private String name;

	private String descr;
	
	@ManyToMany(mappedBy = "trunks")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	Set<Channel> channels = new HashSet<>();

	public TrunkState getState() {
		TrunkRepo trunksStats = (TrunkRepo) AppInitializer.getContext().getBean("trunkRepoImpl");
		return trunksStats.findStateById(getId());
	}
	
	private Oper oper;
	
	public String toStringAll() {
		StringBuilder sb = new StringBuilder("");
		for (Channel ch: channels)
			sb.append(ch.getName());
		return toString() 
				+ " ch: "    + sb.toString() ;
	}
	
	

}
