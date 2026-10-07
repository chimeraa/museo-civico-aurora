package museo.civico.aurora.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import museo.civico.aurora.dto.FeedbackCreateRequest;
import museo.civico.aurora.dto.FeedbackDto;
import museo.civico.aurora.security.AppUserPrincipal;
import museo.civico.aurora.service.FeedbackService;

@RestController
public class FeedbackController {

	private final FeedbackService feedbackService;

	public FeedbackController(FeedbackService feedbackService) {
		this.feedbackService = feedbackService;
	}

	@PostMapping("/api/events/{id}/feedback")
	@ResponseStatus(HttpStatus.CREATED)
	public FeedbackDto create(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id,
			@Valid @RequestBody FeedbackCreateRequest request) {
		return feedbackService.create(principal, id, request);
	}
}
