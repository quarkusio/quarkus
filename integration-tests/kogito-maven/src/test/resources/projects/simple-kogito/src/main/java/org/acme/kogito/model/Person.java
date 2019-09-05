package org.acme.kogito.model;

import java.io.Serializable;

public class Person implements Serializable {

	private static final long serialVersionUID = -571683427125356701L;

	private String name;
	private int age;
	private boolean adult;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public boolean isAdult() {
		return adult;
	}

	public void setAdult(boolean adult) {
		this.adult = adult;
	}

	@Override
	public String toString() {
		return "Person [name=" + name + ", age=" + age + ", adult=" + adult + "]";
	}

}
