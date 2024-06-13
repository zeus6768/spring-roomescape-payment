package roomescape.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

import static roomescape.fixture.TestFixture.ADMIN_EMAIL;
import static roomescape.fixture.TestFixture.ADMIN_NAME;
import static roomescape.fixture.TestFixture.AMOUNT;
import static roomescape.fixture.TestFixture.DATE_MAY_EIGHTH;
import static roomescape.fixture.TestFixture.MEMBER_TENNY_EMAIL;
import static roomescape.fixture.TestFixture.MEMBER_TENNY_NAME;
import static roomescape.fixture.TestFixture.ORDER_ID;
import static roomescape.fixture.TestFixture.PAYMENT_KEY;
import static roomescape.fixture.TestFixture.THEME_HORROR_NAME;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import roomescape.domain.reservation.ReservationStatus;
import roomescape.dto.reservation.AdminReservationSaveRequest;
import roomescape.dto.reservation.ReservationResponse;
import roomescape.dto.reservation.ReservationSaveRequest;

class ReservationAcceptanceTest extends AcceptanceTest {

    @Test
    @DisplayName("사용자가 예약을 성공적으로 생성하면 201을 응답한다.")
    void respondCreatedWhenCreateReservation() {
        final Long timeId = saveReservationTime();
        final Long themeId = saveTheme();
        final ReservationSaveRequest request
                = new ReservationSaveRequest(DATE_MAY_EIGHTH, timeId, themeId, PAYMENT_KEY, ORDER_ID, AMOUNT);

        final ReservationResponse response = assertPostResponseWithToken(
                request, MEMBER_TENNY_EMAIL, "/reservations", 201)
                .extract().as(ReservationResponse.class);

        assertAll(() -> {
            assertThat(response.name()).isEqualTo(MEMBER_TENNY_NAME);
            assertThat(response.date()).isEqualTo(DATE_MAY_EIGHTH);
            assertThat(response.time().id()).isEqualTo(timeId);
            assertThat(response.theme().id()).isEqualTo(themeId);
        });
    }

    @Test
    @DisplayName("관리자가 예약을 성공적으로 생성하면 201을 응답한다.")
    void respondCreatedWhenAdminCreateReservation() {
        final Long timeId = saveReservationTime();
        final Long themeId = saveTheme();
        final AdminReservationSaveRequest request = new AdminReservationSaveRequest(1L, DATE_MAY_EIGHTH, timeId,
                themeId);

        final ReservationResponse response = assertPostResponseWithToken(
                request, ADMIN_EMAIL, "/admin/reservations", 201)
                .extract().as(ReservationResponse.class);

        assertAll(() -> {
            assertThat(response.name()).isEqualTo(ADMIN_NAME);
            assertThat(response.date()).isEqualTo(DATE_MAY_EIGHTH);
            assertThat(response.time().id()).isEqualTo(timeId);
            assertThat(response.theme().id()).isEqualTo(themeId);
        });
    }

    @Test
    @DisplayName("존재하지 않는 예약 시간으로 예약 생성 시 404를 응답한다.")
    void respondBadRequestWhenNotExistingReservationTime() {
        saveReservationTime();
        final Long themeId = saveTheme();
        final ReservationSaveRequest request
                = new ReservationSaveRequest(DATE_MAY_EIGHTH, 10L, themeId, PAYMENT_KEY, ORDER_ID, AMOUNT);

        assertPostResponseWithToken(request, MEMBER_TENNY_EMAIL, "/reservations", 404);
    }

    @Test
    @DisplayName("존재하지 않는 테마로 예약 생성 시 404를 응답한다.")
    void respondBadRequestWhenNotExistingTheme() {
        final ReservationSaveRequest request
                = new ReservationSaveRequest(DATE_MAY_EIGHTH, 1L, 100L, PAYMENT_KEY, ORDER_ID, AMOUNT);

        assertPostResponseWithToken(request, MEMBER_TENNY_EMAIL, "/reservations", 404);
    }

    @Test
    @DisplayName("예약 목록을 성공적으로 조회하면 200을 응답한다.")
    void respondOkWhenFindReservations() {
        final JsonPath jsonPath = assertGetResponse("/reservations", 200)
                .extract().response().jsonPath();

        assertAll(() -> {
            assertThat(jsonPath.getString("name[0]")).isEqualTo(ADMIN_NAME);
            assertThat(jsonPath.getString("date[0]")).isEqualTo("2024-06-01");
            assertThat(jsonPath.getString("time[0].startAt")).isEqualTo("13:00");
            assertThat(jsonPath.getString("theme[0].name")).isEqualTo(THEME_HORROR_NAME);
        });
    }

    @Test
    @DisplayName("테마, 사용자, 예약 날짜로 예약 목록을 성공적으로 조회하면 200을 응답한다.")
    void respondOkWhenFilteredFindReservations() {
        final String accessToken = getAccessToken(ADMIN_EMAIL);

        final JsonPath jsonPath = RestAssured.given().log().all()
                .queryParam("themeId", 1L)
                .queryParam("memberId", 1L)
                .queryParam("dateFrom", "2034-05-01")
                .queryParam("dateTo", "R2034-05-08")
                .cookie("token", accessToken)
                .when().get("/reservations")
                .then().log().all()
                .statusCode(200)
                .extract().response().jsonPath();

        assertAll(() -> {
            assertThat(jsonPath.getString("name[0]")).isEqualTo(ADMIN_NAME);
            assertThat(jsonPath.getString("date[0]")).isEqualTo("2024-06-01");
            assertThat(jsonPath.getString("time[0].startAt")).isEqualTo("13:00");
            assertThat(jsonPath.getString("theme[0].name")).isEqualTo("호러");
        });
    }

    @Test
    @DisplayName("예약을 성공적으로 삭제하면 204를 응답한다.")
    void respondNoContentWhenDeleteReservation() {
        assertDeleteResponse("/reservations/", 1L, 204);
    }

    @Test
    @DisplayName("존재하지 않는 예약을 삭제하면 404를 응답한다.")
    void respondBadRequestWhenDeleteNotExistingReservation() {
        final Long notExistingReservationTimeId = 10L;

        assertDeleteResponse("/reservations/", notExistingReservationTimeId, 404);
    }

    @Test
    @DisplayName("특정 사용자의 예약 및 예약 대기 목록을 성공적으로 조회하면 200을 응답한다.")
    void respondOkWhenFindMyReservations() {
        final String accessToken = getAccessToken(ADMIN_EMAIL);

        final JsonPath jsonPath = assertGetResponseWithToken(accessToken, "/reservations/mine", 200)
                .extract().response().jsonPath();

        assertAll(() -> {
            assertThat(jsonPath.getString("theme[0]")).isEqualTo(THEME_HORROR_NAME);
            assertThat(jsonPath.getString("date[0]")).isEqualTo("2024-06-01");
            assertThat(jsonPath.getString("time[0]")).isEqualTo("13:00");
            assertThat(jsonPath.getString("status[0]")).isEqualTo(ReservationStatus.RESERVED.value());
        });
    }
}
