package museo.civico.aurora.dto;

/** Indicatori aggregati amministrativi. */
public record AdminStatsDto(long publishedEventCount, long upcomingEventCount, long confirmedBookingCount,
		double capacityUtilizationPercent, Double averageRating, long feedbackCount) {
}
