package homdork.code.model;

public abstract class Device {
	public String id;
	public State state;  // "on" and "off" for all device types.
	public DeviceType deviceType;  // "LAMP","FAN" , "THERMOMETER", "CURTAIN" ...
	public String userId;
	public double level;  // brightness : Lamp[ceiling + floor] . Speed : Fan . Warmth : Thermometer
	public int pin;

	public DeviceType getDeviceType() {
		return deviceType;
	}

	// for some reasons we have this here, but I -@Willz think it will be a time saver soon.
	public void setDeviceType(DeviceType deviceType) {
		this.deviceType = deviceType;
	}

	public State getState() {
		return state;
	}

	public void turnOff() {
		this.state = State.OFF;
	}

	public void turnOn() {
		this.state = State.ON;
	}

	public String getId() {
		return id;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}

	public void setState(State state) {
		this.state = state;
	}

	public double getLevel() {
		return level;
	}

	public void setLevel(double level) {
		this.level = level;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Override
	public String toString() {
		return "Device{" +
				"id='" + id + '\'' +
				", state=" + state +
				", deviceType=" + deviceType +
				", userId='" + userId + '\'' +
				", level=" + level +
				'}';
	}

	public int getPin() {
		return pin;
	}

	public void setPin(int pin) {
		this.pin = pin;
	}
}
