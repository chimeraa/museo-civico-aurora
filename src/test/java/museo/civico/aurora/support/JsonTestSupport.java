package museo.civico.aurora.support;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Piccole funzioni per leggere un valore dal JSON nei test.
public final class JsonTestSupport {

	private JsonTestSupport() {
	}

	// Legge un numero dal JSON.
	public static long extractLong(String json, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*(\\d+)").matcher(json);
		if (matcher.find()) {
			return Long.parseLong(matcher.group(1));
		}
		throw new IllegalArgumentException("Campo numerico '" + field + "' assente nel JSON: " + json);
	}

	// Legge una stringa dal JSON.
	public static String extractString(String json, String field) {
		Matcher matcher = Pattern.compile("\"" + Pattern.quote(field) + "\"\\s*:\\s*\"([^\"]*)\"").matcher(json);
		if (matcher.find()) {
			return matcher.group(1);
		}
		throw new IllegalArgumentException("Campo stringa '" + field + "' assente nel JSON: " + json);
	}
}
