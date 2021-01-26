package io.webdirscan.internal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import static io.webdirscan.internal.Constants.LOGGER;

public class EventBus {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	public static @interface Subscribe {
	}

	private static EventBus instance;
	private Map<String, Class> subscribersWithTag = new HashMap<String, Class>();

	private EventBus() {
	}

	private synchronized static EventBus getInstance0() {
		if (instance == null) {
			instance = new EventBus();
		}
		return instance;
	}

	public static EventBus getInstance() {
		return getInstance0();
	}

	/**
	 * @param tag   订阅者标记
	 * @param clazz
	 * @return
	 */
	public EventBus register(String tag, Class clazz) {
		Objects.requireNonNull(clazz, "class must be not null");
		if (subscribersWithTag.get(tag) != null) {
			try {
				throw new Exception("tag has been used");
			} catch (Exception e) {
				LOGGER.warning(tag + ":" + clazz.getName() + " register fail");
			}
		} else {
			subscribersWithTag.put(tag, clazz);
		}
		return this;
	}

	/**
	 * @param clazz
	 * @return
	 */
	public EventBus register(Class clazz) {
		String tag = clazz.getName() + ":" + System.currentTimeMillis() + ":" + clazz.hashCode();
		return register(tag, clazz);
	}

	/**
	 * @param tag
	 * @return
	 */
	public EventBus remove(String... tag) {
		for (String tag0 : tag)
			subscribersWithTag.remove(tag0);
		return this;
	}

	public EventBus remove(Class clazz) {
		for (Iterator<String> subscribers = subscribersWithTag.keySet().iterator(); subscribers.hasNext();) {
			String tag = subscribers.next();
			Class clazz0 = subscribersWithTag.get(tag);
			if (clazz0.getName().equals(clazz.getName()))
				subscribersWithTag.remove(tag);
		}
		return this;
	}

	private EventBus notify(Class clazz) {
		return notify(clazz, null);
	}

	private EventBus notify(Class clazz, Object msg) {
		try {
			Object instance = clazz.newInstance();
			for (Method method : clazz.getDeclaredMethods()) {
				if (method.isAnnotationPresent(Subscribe.class))
					method.invoke(instance, msg);
			}
		} catch (Exception e) {
			LOGGER.warning(e.getMessage());
		}
		return this;
	}

	public EventBus notifyByTag(String tag, Object msg) {
		Class subscriber = subscribersWithTag.get(tag);
		if (subscriber != null)
			notify(subscriber,msg);
		return this;
	}

	public EventBus notifyByAll(Object msg) {
		for (Iterator<Class> subscribers = subscribersWithTag.values().iterator(); subscribers.hasNext();) {
			Class clazz = subscribers.next();
			if (clazz != null) {
				notify(clazz, msg);
			}
		}
		return this;
	}
}
