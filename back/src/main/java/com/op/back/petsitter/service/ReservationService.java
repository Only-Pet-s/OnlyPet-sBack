package com.op.back.petsitter.service;

import com.op.back.petsitter.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationCommandService reservationCommandService;
    private final ReservationQueryService reservationQueryService;

    public String createReservation(String uid, ReservationRequestDTO req) {
        return reservationCommandService.createReservation(uid, req);
    }

    public AvailableTimeResponseDTO getAvailableTimes(String petsitterId, String date) {
        return reservationQueryService.getAvailableTimes(petsitterId, date);
    }

    public CancelReservationResponseDTO cancelReservation(
            String reservationId,
            String uid
    ) throws Exception {
        return reservationCommandService.cancelReservation(reservationId, uid);
    }

    public List<ReadUserReservationDTO> getUserReservation(String uid) {
        return reservationQueryService.getUserReservation(uid);
    }

    public List<ReadPetsitterReservedDTO> getPetsitterReserved(String petsitterId) {
        return reservationQueryService.getPetsitterReserved(petsitterId);
    }

    public PetsitterReservationCountDTO getReservationCount(String petsitterId) {
        return reservationQueryService.getReservationCount(petsitterId);
    }

    public PetsitterRevenueDTO getTotalRevenue(String petsitterId) {
        return reservationQueryService.getTotalRevenue(petsitterId);
    }

    public ScheduleWeekDTO getScheduleWeek(String petsitterId) {
        return reservationQueryService.getScheduleWeek(petsitterId);
    }

    public void acceptReservation(String petsitterId, String reservationId) {
        reservationCommandService.acceptReservation(petsitterId, reservationId);
    }

    public void rejectReservation(String petsitterUid, String reservationId) {
        reservationCommandService.rejectReservation(petsitterUid, reservationId);
    }
}
