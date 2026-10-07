package museo.civico.aurora.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Estrazione minima di valori numerici da JSON nei test. */
public final class JsonTestSupport {

	private JsonTestSupport() {
	}

	public static long extractLong(String json, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)").matcher(json);
		if (matcher.find()) {
			return Long.parseLong(matcher.group(1));
		}
		throw new IllegalArgumentException("Campo numerico '" + field + "' assente nel JSON: " + json);
	}

	public static String extractString(String json, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("Campo stringa '" + field + "' assente nel JSON: " + json);
	}
}
