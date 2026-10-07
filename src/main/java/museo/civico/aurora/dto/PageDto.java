package museo.civico.aurora.dto;

import java.util.List;

import org.springframework.data.domain.Page;

/**
 * DTO di pagina del contratto: indici 0-based.
 */
public record PageDto<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <T> PageDto<T> of(Page<?> source, List<T> content) {
		return new PageDto<>(content, source.getNumber(), source.getSize(), source.getTotalElements(),
				source.getTotalPages());
	}
}
