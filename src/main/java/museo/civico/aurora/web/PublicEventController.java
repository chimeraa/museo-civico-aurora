package museo.civico.aurora.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import museo.civico.aurora.dto.EventDetailDto;
import museo.civico.aurora.dto.EventSummaryDto;
import museo.civico.aurora.dto.PageDto;
import museo.civico.aurora.service.EventService;

@RestController
@RequestMapping("/api/events")
public class PublicEventController {

	private final EventService eventService;

	public PublicEventController(EventService eventService) {
		this.eventService = eventService;
	}

	@GetMapping
	public PageDto<EventSummaryDto> list(@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to) {
		return eventService.listPublic(page, size, from, to);
	}

	@GetMapping("/{id}")
	public EventDetailDto detail(@PathVariable Long id) {
		return eventService.getPublicDetail(id);
	}
}
