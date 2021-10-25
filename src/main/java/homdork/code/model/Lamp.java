package homdork.code.model;

import java.util.UUID;

public class Lamp extends Device {

	public Lamp(String id) {
		this.id = id;
		this.state = State.OFF;
	}
}
