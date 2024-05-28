package roomescape.dto.reservation;

import roomescape.domain.reservation.Reservation;
import roomescape.domain.reservation.ReservationStatus;
import roomescape.dto.theme.ReservedThemeResponse;

import java.time.LocalDate;

public record ReservationResponse(
        Long id,
        String name,
        LocalDate date,
        ReservationTimeResponse time,
        ReservedThemeResponse theme,
        ReservationStatus status
) {

    public static ReservationResponse from(final Reservation reservation) {
        return new ReservationResponse(
                reservation.getId(),
                reservation.getMemberName(),
                reservation.getDate(),
                ReservationTimeResponse.from(reservation.getTime()),
                ReservedThemeResponse.from(reservation.getTheme()),
                reservation.getStatus()
        );
    }
}