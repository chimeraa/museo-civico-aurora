package museo.civico.aurora.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import museo.civico.aurora.dto.AdminBookingDto;
import museo.civico.aurora.dto.AdminEventSummaryDto;
import museo.civico.aurora.dto.EventCreateRequest;
import museo.civico.aurora.dto.EventDetailDto;
import museo.civico.aurora.dto.EventUpdateRequest;
import museo.civico.aurora.dto.FeedbackDto;
import museo.civico.aurora.dto.PageDto;
import museo.civico.aurora.service.BookingService;
import museo.civico.aurora.service.EventService;
import museo.civico.aurora.service.FeedbackService;

@RestController
@RequestMapping("/api/admin")
public class AdminEventController {

	private final EventService eventService;
	private final BookingService bookingService;
	private final FeedbackService feedbackService;

	public AdminEventController(EventService eventService, BookingService bookingService,
			FeedbackService feedbackService) {
		this.eventService = eventService;
		this.bookingService = bookingService;
		this.feedbackService = feedbackService;
	}

	@GetMapping("/events")
	public PageDto<AdminEventSummaryDto> list(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return eventService.listAdmin(page, size);
	}

	@GetMapping("/events/{id}")
	public EventDetailDto detail(@PathVariable Long id) {
		return eventService.getAdminDetail(id);
	}

	@PostMapping("/events")
	@ResponseStatus(HttpStatus.CREATED)
	public EventDetailDto create(@Valid @RequestBody EventCreateRequest request) {
		return eventService.create(request);
	}

	@PutMapping("/events/{id}")
	public EventDetailDto update(@PathVariable Long id, @Valid @RequestBody EventUpdateRequest request) {
		return eventService.update(id, request);
	}

	@PostMapping("/events/{id}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable Long id) {
		eventService.cancel(id);
	}

	@GetMapping("/events/{id}/bookings")
	public PageDto<AdminBookingDto> bookings(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return bookingService.listForEvent(id, page, size);
	}

	@GetMapping("/events/{id}/feedback")
	public PageDto<FeedbackDto> feedback(@PathVariable Long id, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return feedbackService.listForEvent(id, page, size);
	}
}
