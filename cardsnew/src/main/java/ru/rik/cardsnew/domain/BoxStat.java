package ru.rik.cardsnew.domain;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode(of={"id", "name"})
public class BoxStat implements State {
	private long id;
	private String name;
	
	public BoxStat() {
	}

	@Override public long getId() {return id;}
	@Override public void setId(long id) { this.id = id;}
	
	@Override public String getName() {return name;}
	@Override public void setName(String name) {this.name = name;}

	@Override
	public Class<?> getClazz() {
		return BoxStat.class;
	}
}
