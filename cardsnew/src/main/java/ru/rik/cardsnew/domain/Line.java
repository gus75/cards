package ru.rik.cardsnew.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
@AllArgsConstructor
public enum Line {
	L1(1, 8080, 8023, 2),
	L2(2, 8080, 8023, 1),
	L3(3, 8180, 8123, 4),
	L4(4, 8180, 8123, 3),
	L5(5, 8280, 8323, 6),
	L6(6, 8280, 8323, 5),
	L7(7, 8380, 8423, 8),
	L8(8, 8480, 8423, 7);
	
	@Getter
	private final int position;
	@Getter
	private final int httpport;
	@Getter
	private final int telnetport;
	@Getter
	private final int peer;
	
	public static Line getInstance(int p) {
		switch (p) {
		case 1: return L1;
		case 2:	return L2;
		case 3: return L3;
		case 4:	return L4;
		case 5:	return L5;
		case 6:	return L6;
		case 7:	return L7;
		case 8:	return L8;
		default: throw new IllegalArgumentException("There is no such Line with position " + p);
		}
	}
}
