package museo.civico.aurora.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import museo.civico.aurora.dto.AdminStatsDto;
import museo.civico.aurora.service.StatsService;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

	private final StatsService statsService;

	public AdminStatsController(StatsService statsService) {
		this.statsService = statsService;
	}

	@GetMapping
	public AdminStatsDto stats() {
		return statsService.getStats();
	}
}
