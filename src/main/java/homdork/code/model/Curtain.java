package homdork.code.model;

import java.util.UUID;

public class Curtain extends Device {

	public Curtain(String id) {
		this.id = id;
		this.state = State.OFF;
	}
}
