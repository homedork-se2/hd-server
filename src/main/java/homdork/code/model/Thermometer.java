package homdork.code.model;

import java.util.UUID;

public class Thermometer extends Device {


	public Thermometer(String id) {
		this.id = id;
		this.state = State.OFF;
	}

}
