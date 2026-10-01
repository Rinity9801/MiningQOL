package forfun.miningqol.client.shatter;

/** A value that eases between 0 and 1 over a fixed duration. Retargeting mid-flight starts from the current value. */
public final class Anim {
	private final long duration;
	private float from, to;
	private long startedAt = -1;

	public Anim(long durationMs, boolean initiallyOn) {
		this.duration = durationMs;
		this.from = this.to = initiallyOn ? 1f : 0f;
	}

	public void set(boolean on) {
		float target = on ? 1f : 0f;
		if (target == to) return;
		from = value();
		to = target;
		startedAt = System.currentTimeMillis();
	}

	public boolean target() {
		return to == 1f;
	}

	public boolean isAnimating() {
		return startedAt >= 0 && System.currentTimeMillis() - startedAt < duration;
	}

	/** Eased 0..1. */
	public float value() {
		if (startedAt < 0) return to;
		float t = Math.min(1f, (System.currentTimeMillis() - startedAt) / (float) duration);
		float e = 1f - (float) Math.pow(1f - t, 3); // ease-out cubic
		return from + (to - from) * e;
	}

	public float lerp(float off, float on) {
		return off + (on - off) * value();
	}
}
