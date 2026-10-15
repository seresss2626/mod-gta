package dev.gtacraft;

/** Типы машин. Для каждого есть предмет-кузов и модель в assets. */
public enum CarType {
	BUGGY("buggy", "Багги", 0.70, 0.030, 4.2f),
	SPORT("sport", "Спорткар", 0.98, 0.042, 3.6f),
	PICKUP("pickup", "Пикап", 0.62, 0.026, 3.4f),
	TAXI("taxi", "Такси", 0.72, 0.032, 3.8f),
	POLICE("police", "Полицейская", 0.92, 0.040, 4.0f);

	public final String id, title;
	public final double maxSpeed, accel;
	public final float turn;

	CarType(String id, String title, double maxSpeed, double accel, float turn) {
		this.id = id;
		this.title = title;
		this.maxSpeed = maxSpeed;
		this.accel = accel;
		this.turn = turn;
	}

	public String bodyItem() {
		return GtaCraft.MODID + ":" + id + "_body";
	}

	public String tag() {
		return "cartype_" + id;
	}

	public static CarType byId(String s) {
		for (CarType t : values()) {
			if (t.id.equals(s)) {
				return t;
			}
		}
		return null;
	}

	public static CarType ofTags(java.util.Set<String> tags) {
		for (CarType t : values()) {
			if (tags.contains(t.tag())) {
				return t;
			}
		}
		return BUGGY;
	}
}
