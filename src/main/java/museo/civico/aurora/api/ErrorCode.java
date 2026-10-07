package museo.civico.aurora.api;

/**
 * Codici applicativi dell'error envelope. Devono restare allineati al contratto
 * {@code docs/api-contract.md}.
 */
public final class ErrorCode {

	public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
	public static final String INVALID_PAGE = "INVALID_PAGE";
	public static final String INVALID_PAGE_SIZE = "INVALID_PAGE_SIZE";
	public static final String INVALID_TIME_RANGE = "INVALID_TIME_RANGE";
	public static final String EVENT_ALREADY_STARTED = "EVENT_ALREADY_STARTED";
	public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
	public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
	public static final String FORBIDDEN = "FORBIDDEN";
	public static final String CSRF_INVALID = "CSRF_INVALID";
	public static final String NOT_FOUND = "NOT_FOUND";
	public static final String EVENT_FULL = "EVENT_FULL";
	public static final String ALREADY_BOOKED = "ALREADY_BOOKED";
	public static final String FEEDBACK_EXISTS = "FEEDBACK_EXISTS";
	public static final String EMAIL_EXISTS = "EMAIL_EXISTS";
	public static final String INVALID_EVENT_STATE = "INVALID_EVENT_STATE";
	public static final String CAPACITY_BELOW_BOOKINGS = "CAPACITY_BELOW_BOOKINGS";
	public static final String BOOKING_ALREADY_STARTED = "BOOKING_ALREADY_STARTED";
	public static final String EVENT_CANNOT_BE_CANCELLED = "EVENT_CANNOT_BE_CANCELLED";
	public static final String UNSUPPORTED_MEDIA_TYPE = "UNSUPPORTED_MEDIA_TYPE";
	public static final String CONFLICT = "CONFLICT";
	public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

	private ErrorCode() {
	}
}
