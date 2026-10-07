package museo.civico.aurora;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import museo.civico.aurora.api.ApiException;
import museo.civico.aurora.domain.AppUser;
import museo.civico.aurora.domain.BookingStatus;
import museo.civico.aurora.domain.CulturalEvent;
import museo.civico.aurora.dto.BookingCreateRequest;
import museo.civico.aurora.security.AppUserPrincipal;
import museo.civico.aurora.service.BookingService;
import museo.civico.aurora.support.AbstractIntegrationTest;

/**
 * T1 (nessun overbooking con M richieste concorrenti per capienza N) e T2
 * (doppia prenotazione attiva in race: unique violation DB mappata a 409).
 * Usa transazioni e thread distinti, senza transazione esterna del test.
 */
class BookingConcurrencyTest extends AbstractIntegrationTest {

	@Autowired
	private BookingService bookingService;

	@Test
	void concurrentBookingsNeverExceedCapacity() throws Exception {
		int capacity = 3;
		int attempts = 8;
		CulturalEvent event = persistPublishedFutureEvent("Concorrenza capienza", capacity);

		List<AppUser> visitors = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			visitors.add(createVisitor("concurrent" + i + "@example.com"));
		}

		Map<String, AtomicInteger> outcomes = new ConcurrentHashMap<>();
		ExecutorService executor = Executors.newFixedThreadPool(attempts);
		try {
			List<Future<Void>> futures = new ArrayList<>();
			CountDownLatch start = new CountDownLatch(1);
			for (AppUser visitor : visitors) {
				AppUserPrincipal principal = principalOf(visitor);
				Callable<Void> task = () -> {
					start.await(10, TimeUnit.SECONDS);
					try {
						bookingService.create(principal, new BookingCreateRequest(event.getId()));
						outcomes.computeIfAbsent("OK", key -> new AtomicInteger()).incrementAndGet();
					}
					catch (ApiException ex) {
						outcomes.computeIfAbsent(ex.getCode(), key -> new AtomicInteger()).incrementAndGet();
					}
					return null;
				};
				futures.add(executor.submit(task));
			}
			start.countDown();
			for (Future<Void> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			executor.shutdownNow();
		}

		long confirmed = bookingRepository.countByEventIdAndStatus(event.getId(), BookingStatus.CONFIRMED);
		assertThat(confirmed).isEqualTo(capacity);
		assertThat(outcomes.getOrDefault("OK", new AtomicInteger()).get()).isEqualTo(capacity);
		assertThat(outcomes.getOrDefault("EVENT_FULL", new AtomicInteger()).get()).isEqualTo(attempts - capacity);
	}

	@Test
	void concurrentDuplicateBookingsProduceSingleActiveBooking() throws Exception {
		CulturalEvent event = persistPublishedFutureEvent("Concorrenza duplicati", 5);
		AppUser visitor = createVisitor("race@example.com");
		AppUserPrincipal principal = principalOf(visitor);

		AtomicInteger successes = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			CountDownLatch start = new CountDownLatch(1);
			List<Callable<Void>> tasks = List.of(() -> book(principal, event.getId(), start, successes, conflicts),
					() -> book(principal, event.getId(), start, successes, conflicts));
			List<Future<Void>> futures = new ArrayList<>();
			for (Callable<Void> task : tasks) {
				futures.add(executor.submit(task));
			}
			start.countDown();
			for (Future<Void> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			executor.shutdownNow();
		}

		assertThat(successes.get()).isEqualTo(1);
		assertThat(conflicts.get()).isEqualTo(1);
		assertThat(bookingRepository.countByEventIdAndStatus(event.getId(), BookingStatus.CONFIRMED)).isEqualTo(1);
	}

	private Void book(AppUserPrincipal principal, Long eventId, CountDownLatch start, AtomicInteger successes,
			AtomicInteger conflicts) throws InterruptedException {
		start.await(10, TimeUnit.SECONDS);
		try {
			bookingService.create(principal, new BookingCreateRequest(eventId));
			successes.incrementAndGet();
		}
		catch (ApiException ex) {
			assertThat(ex.getCode()).isEqualTo("ALREADY_BOOKED");
			conflicts.incrementAndGet();
		}
		return null;
	}
}
