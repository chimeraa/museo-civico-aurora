package museo.civico.aurora.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import museo.civico.aurora.service.BookingService;

@RestController
@RequestMapping("/api/admin/bookings")
public class AdminBookingController {

	private final BookingService bookingService;

	public AdminBookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping("/{id}/cancel")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void cancel(@PathVariable Long id) {
		bookingService.cancelByAdmin(id);
	}
}
