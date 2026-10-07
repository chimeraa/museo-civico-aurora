package museo.civico.aurora.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import museo.civico.aurora.dto.BookingCreateRequest;
import museo.civico.aurora.dto.BookingDto;
import museo.civico.aurora.dto.PageDto;
import museo.civico.aurora.security.AppUserPrincipal;
import museo.civico.aurora.service.BookingService;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public BookingDto create(@AuthenticationPrincipal AppUserPrincipal principal,
			@Valid @RequestBody BookingCreateRequest request) {
		return bookingService.create(principal, request);
	}

	@GetMapping("/me")
	public PageDto<BookingDto> mine(@AuthenticationPrincipal AppUserPrincipal principal,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "20") int size) {
		return bookingService.listMine(principal, page, size);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@AuthenticationPrincipal AppUserPrincipal principal, @PathVariable Long id) {
		bookingService.cancelMine(principal, id);
	}
}
