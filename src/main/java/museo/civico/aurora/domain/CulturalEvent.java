package museo.civico.aurora.domain;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cultural_event")
public class CulturalEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 160)
	private String title;

	@Column(name = "description", length = 5000)
	private String description;

	@Column(name = "location", nullable = false, length = 200)
	private String location;

	@Column(name = "starts_at", nullable = false)
	private OffsetDateTime startsAt;

	@Column(name = "ends_at", nullable = false)
	private OffsetDateTime endsAt;

	@Column(name = "capacity", nullable = false)
	private Integer capacity;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private EventStatus status;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected CulturalEvent() {
		// richiesto da JPA
	}

	public CulturalEvent(String title, String description, String location, OffsetDateTime startsAt,
			OffsetDateTime endsAt, Integer capacity, EventStatus status, OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
		this.title = title;
		this.description = description;
		this.location = location;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.capacity = capacity;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public OffsetDateTime getStartsAt() {
		return startsAt;
	}

	public void setStartsAt(OffsetDateTime startsAt) {
		this.startsAt = startsAt;
	}

	public OffsetDateTime getEndsAt() {
		return endsAt;
	}

	public void setEndsAt(OffsetDateTime endsAt) {
		this.endsAt = endsAt;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public EventStatus getStatus() {
		return status;
	}

	public void setStatus(EventStatus status) {
		this.status = status;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
