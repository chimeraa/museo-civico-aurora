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
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.domain.EventStatus;
import museo.civico.aurora.domain.Feedback;
import museo.civico.aurora.dto.FeedbackCreateRequest;
import museo.civico.aurora.dto.FeedbackDto;
import museo.civico.aurora.dto.PageDto;
import museo.civico.aurora.repository.AppUserRepository;
import museo.civico.aurora.repository.CulturalEventRepository;
import museo.civico.aurora.repository.FeedbackRepository;
import museo.civico.aurora.security.AppUserPrincipal;

@Service
public class FeedbackService {

	private static final Sort CREATED_AT_DESC = Sort.by(Sort.Direction.DESC, "createdAt");

	private final FeedbackRepository feedbacks;
	private final CulturalEventRepository events;
	private final AppUserRepository users;
	private final Clock clock;

	public FeedbackService(FeedbackRepository feedbacks, CulturalEventRepository events, AppUserRepository users,
			Clock clock) {
		this.feedbacks = feedbacks;
		this.events = events;
		this.users = users;
		this.clock = clock;
	}

	@Transactional
	public FeedbackDto create(AppUserPrincipal principal, Long eventId, FeedbackCreateRequest request) {
		CulturalEvent event = events.findById(eventId).orElseThrow(FeedbackService::notFound);
		if (event.getStatus() != EventStatus.PUBLISHED) {
			// Bozze e annullati non sono giudicabili; gli eventi conclusi si'.
			throw notFound();
		}

		AppUser user = users.findById(principal.getId()).orElseThrow(FeedbackService::notFound);
		Feedback feedback = new Feedback(event, user, request.rating(), trimToNull(request.comment()),
				OffsetDateTime.now(clock));
		try {
			feedback = feedbacks.saveAndFlush(feedback);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ApiException(HttpStatus.CONFLICT, ErrorCode.FEEDBACK_EXISTS,
					"Hai gia' lasciato un giudizio per questo evento.");
		}
		return toDto(feedback);
	}

	@Transactional(readOnly = true)
	public PageDto<FeedbackDto> listForEvent(Long eventId, int page, int size) {
		if (!events.existsById(eventId)) {
			throw notFound();
		}
		Pageable pageable = Paging.validate(page, size, CREATED_AT_DESC);
		Page<Feedback> result = feedbacks.findByEventId(eventId, pageable);
		List<FeedbackDto> content = result.getContent().stream().map(FeedbackService::toDto).toList();
		return PageDto.of(result, content);
	}

	private static FeedbackDto toDto(Feedback feedback) {
		return new FeedbackDto(feedback.getId(), feedback.getEvent().getId(), feedback.getUser().getDisplayName(),
				feedback.getRating(), feedback.getComment(), feedback.getCreatedAt());
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
