package museo.civico.aurora.dto;

// I numeri delle statistiche per l'admin.
public record AdminStatsDto(long publishedEventCount, long upcomingEventCount, long confirmedBookingCount,
		double capacityUtilizationPercent, Double averageRating, long feedbackCount) {
}
