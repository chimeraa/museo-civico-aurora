package museo.civico.aurora.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import museo.civico.aurora.api.ApiException;
import museo.civico.aurora.api.ErrorCode;
import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.Booking;
import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.dto.AdminBookingDto;
import museo.civico.aurora.dto.BookingCreateRequest;
import museo.civico.aurora.dto.BookingDto;
import museo.civico.aurora.dto.PageDto;
import museo.civico.aurora.repository.AppUserRepository;
import museo.civico.aurora.repository.BookingRepository;
import museo.civico.aurora.repository.CulturalEventRepository;
import museo.civico.aurora.security.AppUserPrincipal;

@Service
public class BookingService {

	private static final Sort CREATED_AT_DESC = Sort.by(Sort.Direction.DESC, "createdAt");

	private final BookingRepository bookings;
	private final CulturalEventRepository events;
	private final AppUserRepository users;
	private final Clock clock;

	public BookingService(BookingRepository bookings, CulturalEventRepository events, AppUserRepository users,
			Clock clock) {
		this.bookings = bookings;
		this.events = events;
		this.users = users;
		this.clock = clock;
	}

	/**
	 * Prenotazione in un'unica transazione: lock pessimistico della riga evento,
	 * controlli di stato/tempo/capienza, inserimento. Un'eventuale violazione
	 * dell'unicita' attiva concorrente e' mappata a 409, mai a 500.
	 */
	@Transactional
	public BookingDto create(AppUserPrincipal principal, BookingCreateRequest request) {
		CulturalEvent event = events.findByIdForUpdate(request.eventId()).orElse(null);
		if (event == null || event.getStatus() != EventStatus.PUBLISHED) {
			throw notFound();
		}

		OffsetDateTime now = OffsetDateTime.now(clock);
		if (!now.isBefore(event.getStartsAt())) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.EVENT_ALREADY_STARTED,
					"L'evento e' gia' iniziato.");
		}

		long confirmed = bookings.countByEventIdAndStatus(event.getId(), BookingStatus.CONFIRMED);
		if (confirmed >= event.getCapacity()) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.EVENT_FULL,
					"Non ci sono posti disponibili per questo evento.");
		}

		AppUser user = users.findById(principal.getId()).orElseThrow(BookingService::notFound);

		Booking booking = new Booking(event, user, BookingStatus.CONFIRMED, now);
		try {
			booking = bookings.saveAndFlush(booking);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.ALREADY_BOOKED,
					"Hai gia' una prenotazione attiva per questo evento.");
		}
		return toDto(booking);
	}

	@Transactional(readOnly = true)
	public PageDto<BookingDto> listMine(AppUserPrincipal principal, int page, int size) {
		Pageable pageable = Paging.validate(page, size, CREATED_AT_DESC);
		Page<Booking> result = bookings.findByUserId(principal.getId(), pageable);
		List<BookingDto> content = result.getContent().stream().map(BookingService::toDto).toList();
		return PageDto.of(result, content);
	}

	@Transactional
	public void cancelMine(AppUserPrincipal principal, Long bookingId) {
		Booking booking = bookings.findById(bookingId).orElseThrow(BookingService::notFound);
		if (!booking.getUser().getId().equals(principal.getId())) {
			// Non rivelare l'esistenza di ID altrui.
			throw notFound();
		}
		cancel(booking);
	}

	@Transactional(readOnly = true)
	public PageDto<AdminBookingDto> listForEvent(Long eventId, int page, int size) {
		if (!events.existsById(eventId)) {
			throw notFound();
		}
		Pageable pageable = Paging.validate(page, size, CREATED_AT_DESC);
		Page<Booking> result = bookings.findByEventId(eventId, pageable);
		List<AdminBookingDto> content = result.getContent().stream().map(BookingService::toAdminDto).toList();
		return PageDto.of(result, content);
	}

	@Transactional
	public void cancelByAdmin(Long bookingId) {
		cancel(bookings.findById(bookingId).orElseThrow(BookingService::notFound));
	}

	private void cancel(Booking booking) {
		if (booking.getStatus() == BookingStatus.CANCELLED) {
			return; // idempotente: 204 anche dopo l'inizio
		}
		OffsetDateTime now = OffsetDateTime.now(clock);
		if (!now.isBefore(booking.getEvent().getStartsAt())) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.BOOKING_ALREADY_STARTED,
					"L'evento e' gia' iniziato: la prenotazione non puo' piu' essere annullata.");
		}
		booking.setStatus(BookingStatus.CANCELLED);
		booking.setCancelledAt(now);
		bookings.save(booking);
	}

	private static BookingDto toDto(Booking booking) {
		CulturalEvent event = booking.getEvent();
		return new BookingDto(booking.getId(), event.getId(), event.getTitle(), event.getStartsAt(),
				booking.getStatus(), booking.getCreatedAt(), booking.getCancelledAt());
	}

	private static AdminBookingDto toAdminDto(Booking booking) {
		CulturalEvent event = booking.getEvent();
		return new AdminBookingDto(booking.getId(), event.getId(), event.getTitle(), event.getStartsAt(),
				booking.getStatus(), booking.getCreatedAt(), booking.getCancelledAt(),
				booking.getUser().getDisplayName());
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Risorsa non trovata.");
	}
}
