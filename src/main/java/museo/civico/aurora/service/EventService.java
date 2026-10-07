package museo.civico.aurora.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import museo.civico.aurora.api.ApiException;
import museo.civico.aurora.api.ErrorCode;
import museo.civico.aurora.api.FieldError;
import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.dto.AdminEventSummaryDto;
import museo.civico.aurora.dto.EventCreateRequest;
import museo.civico.aurora.dto.EventDetailDto;
import museo.civico.aurora.dto.EventSummaryDto;
import museo.civico.aurora.dto.EventUpdateRequest;
import museo.civico.aurora.dto.PageDto;
import museo.civico.aurora.repository.BookingRepository;
import museo.civico.aurora.repository.CulturalEventRepository;

@Service
public class EventService {

	private static final Sort STARTS_AT_ASC = Sort.by(Sort.Direction.ASC, "startsAt");

	private final CulturalEventRepository events;
	private final BookingRepository bookings;
	private final Clock clock;

	public EventService(CulturalEventRepository events, BookingRepository bookings, Clock clock) {
		this.events = events;
		this.bookings = bookings;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	public PageDto<EventSummaryDto> listPublic(int page, int size, String from, String to) {
		Pageable pageable = Paging.validate(page, size, STARTS_AT_ASC);
		OffsetDateTime fromTs = Paging.parseOptional(from, "from");
		OffsetDateTime toTs = Paging.parseOptional(to, "to");
		Paging.validateRange(fromTs, toTs);

		Page<CulturalEvent> result;
		if (fromTs == null && toTs == null) {
			result = events.findByStatus(EventStatus.PUBLISHED, pageable);
		}
		else if (fromTs == null) {
			result = events.findByStatusAndStartsAtLessThanEqual(EventStatus.PUBLISHED, toTs, pageable);
		}
		else if (toTs == null) {
			result = events.findByStatusAndStartsAtGreaterThanEqual(EventStatus.PUBLISHED, fromTs, pageable);
		}
		else {
			result = events.findByStatusAndStartsAtBetween(EventStatus.PUBLISHED, fromTs, toTs, pageable);
		}

		List<EventSummaryDto> content = result.getContent().stream().map(this::toSummary).toList();
		return PageDto.of(result, content);
	}

	@Transactional(readOnly = true)
	public EventDetailDto getPublicDetail(Long id) {
		CulturalEvent event = events.findById(id)
				.filter(e -> e.getStatus() == EventStatus.PUBLISHED)
				.orElseThrow(() -> notFound());
		return toDetail(event);
	}

	@Transactional(readOnly = true)
	public PageDto<AdminEventSummaryDto> listAdmin(int page, int size) {
		Pageable pageable = Paging.validate(page, size, STARTS_AT_ASC);
		Page<CulturalEvent> result = events.findAll(pageable);
		List<AdminEventSummaryDto> content = result.getContent().stream().map(this::toAdminSummary).toList();
		return PageDto.of(result, content);
	}

	@Transactional(readOnly = true)
	public EventDetailDto getAdminDetail(Long id) {
		return toDetail(events.findById(id).orElseThrow(EventService::notFound));
	}

	@Transactional
	public EventDetailDto create(EventCreateRequest request) {
		EventStatus status = request.status() == null ? EventStatus.DRAFT : request.status();
		validateEditable(status, request.startsAt(), request.endsAt());
		OffsetDateTime now = OffsetDateTime.now(clock);
		CulturalEvent event = new CulturalEvent(request.title().trim(), trimToNull(request.description()),
				request.location().trim(), request.startsAt(), request.endsAt(), request.capacity(), status, now, now);
		return toDetail(events.save(event));
	}

	@Transactional
	public EventDetailDto update(Long id, EventUpdateRequest request) {
		CulturalEvent event = events.findByIdForUpdate(id).orElseThrow(EventService::notFound);

		if (event.getStatus() == EventStatus.CANCELLED) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.INVALID_EVENT_STATE,
					"L'evento annullato non puo' essere modificato.");
		}

		validateEditable(request.status(), request.startsAt(), request.endsAt());
		if (event.getStatus() == EventStatus.PUBLISHED && request.status() == EventStatus.DRAFT) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.INVALID_EVENT_STATE,
					"Un evento pubblicato non puo' tornare in bozza.");
		}

		long confirmed = bookings.countByEventIdAndStatus(id, BookingStatus.CONFIRMED);
		if (request.capacity() < confirmed) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.CAPACITY_BELOW_BOOKINGS,
					"La capienza non puo' essere inferiore alle prenotazioni confermate.");
		}

		event.setTitle(request.title().trim());
		event.setDescription(trimToNull(request.description()));
		event.setLocation(request.location().trim());
		event.setStartsAt(request.startsAt());
		event.setEndsAt(request.endsAt());
		event.setCapacity(request.capacity());
		event.setStatus(request.status());
		event.setUpdatedAt(OffsetDateTime.now(clock));
		return toDetail(events.save(event));
	}

	@Transactional
	public void cancel(Long id) {
		CulturalEvent event = events.findByIdForUpdate(id).orElseThrow(EventService::notFound);
		if (event.getStatus() == EventStatus.CANCELLED) {
			return; // idempotente: 204 anche se gia' annullato
		}
		OffsetDateTime now = OffsetDateTime.now(clock);
		if (!now.isBefore(event.getStartsAt())) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.EVENT_CANNOT_BE_CANCELLED,
					"L'evento e' gia' iniziato e non puo' essere annullato.");
		}
		event.setStatus(EventStatus.CANCELLED);
		event.setUpdatedAt(now);
		events.save(event);
		bookings.cancelConfirmedByEventId(id, BookingStatus.CONFIRMED, BookingStatus.CANCELLED, now);
	}

	private void validateEditable(EventStatus status, OffsetDateTime startsAt, OffsetDateTime endsAt) {
		if (startsAt.isAfter(endsAt)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
					"Alcuni campi non sono validi.",
					List.of(new FieldError("endsAt", "La fine non puo' precedere l'inizio.")));
		}
		if (status == EventStatus.CANCELLED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
					"Alcuni campi non sono validi.",
					List.of(new FieldError("status", "Usa l'endpoint di annullamento per cancellare l'evento.")));
		}
	}

	private EventSummaryDto toSummary(CulturalEvent event) {
		return new EventSummaryDto(event.getId(), event.getTitle(), event.getLocation(), event.getStartsAt(),
				event.getEndsAt(), seatsAvailable(event));
	}

	private AdminEventSummaryDto toAdminSummary(CulturalEvent event) {
		return new AdminEventSummaryDto(event.getId(), event.getTitle(), event.getLocation(), event.getStartsAt(),
				event.getEndsAt(), seatsAvailable(event), event.getStatus());
	}

	private EventDetailDto toDetail(CulturalEvent event) {
		return new EventDetailDto(event.getId(), event.getTitle(), event.getLocation(), event.getStartsAt(),
				event.getEndsAt(), seatsAvailable(event), event.getDescription(), event.getCapacity(),
				event.getStatus());
	}

	private int seatsAvailable(CulturalEvent event) {
		long confirmed = bookings.countByEventIdAndStatus(event.getId(), BookingStatus.CONFIRMED);
		return (int) Math.max(0, event.getCapacity() - confirmed);
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	private static ApiException notFound() {
		return new ApiException(HttpStatus.NOT_FOUND, ErrorCode.NOT_FOUND, "Risorsa non trovata.");
	}
}
