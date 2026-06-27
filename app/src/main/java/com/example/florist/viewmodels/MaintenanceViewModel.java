package com.example.florist.viewmodels;

import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.florist.model.MaintenanceTaskUIModel;
import com.example.florist.model.Rental;
import com.example.florist.repository.MaintenanceRepository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MaintenanceViewModel extends ViewModel {
    private final MaintenanceRepository repository = new MaintenanceRepository();

    private final MutableLiveData<List<Rental>> allRentals = new MutableLiveData<>();
    private final MutableLiveData<List<Rental>> filteredRentals = new MutableLiveData<>();
    private final MutableLiveData<List<MaintenanceTaskUIModel>> filteredTasks = new MutableLiveData<>();
    private final MutableLiveData<Date> selectedDate = new MutableLiveData<>();
    private final MutableLiveData<CalendarIndicators> calendarIndicators = new MutableLiveData<>();

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> actionSuccessMessage = new MutableLiveData<>();

    public LiveData<List<Rental>> getFilteredRentals() { return filteredRentals; }
    public LiveData<List<MaintenanceTaskUIModel>> getFilteredTasks() {return filteredTasks;}
    public LiveData<CalendarIndicators> getCalendarIndicators() { return calendarIndicators; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<String> getActionSuccessMessage() { return actionSuccessMessage; }

    public void setSelectedDate(Date date) {
        selectedDate.setValue(date);
        applyDateFilter();
    }

    public static class CalendarIndicators {
        public List<String> routineDates = new ArrayList<>();
        public List<String> complaintDates = new ArrayList<>();
    }

    public void fetchSellerRentals(String status) {
        isLoading.setValue(true);
        repository.getSellerMaintenanceSchedule(status, new MaintenanceRepository.RentalListCallback() {
            @Override
            public void onSuccess(List<Rental> rentals) {
                isLoading.setValue(false);
                allRentals.setValue(rentals);
                applyDateFilter();
                generateCalendarIndicators();
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    private void applyDateFilter() {
        List<Rental> rentals = allRentals.getValue();
        Date targetDate = selectedDate.getValue();

        if (rentals == null || targetDate == null) return;

        List<MaintenanceTaskUIModel> filteredList = new ArrayList<>();

        Calendar todayCal = Calendar.getInstance();
        todayCal.set(Calendar.HOUR_OF_DAY, 0); todayCal.set(Calendar.MINUTE, 0); todayCal.set(Calendar.SECOND, 0); todayCal.set(Calendar.MILLISECOND, 0);

        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);
        targetCal.set(Calendar.HOUR_OF_DAY, 0); targetCal.set(Calendar.MINUTE, 0); targetCal.set(Calendar.SECOND, 0); targetCal.set(Calendar.MILLISECOND, 0);

        // KUNCI UX: Apakah user sedang melihat jadwal HARI INI?
        boolean isTargetToday = targetCal.equals(todayCal);

        for (Rental rental : rentals) {
            String status = rental.getStatus() != null ? rental.getStatus().toUpperCase() : "";

            // Perbaikan untuk membaca "AKTIF" (dari data lama) maupun "SEWA AKTIF"
            boolean isRentActive = status.equals("SEWA AKTIF") || status.equals("AKTIF");
            boolean isRetrieval = status.equals("MENUNGGU PENARIKAN");

            String activeComplaintId = rental.getActiveComplaintId();
            boolean isComplaint = (activeComplaintId != null && !activeComplaintId.isEmpty());

            boolean isMaintDay = isMaintenanceDay(rental, targetDate);
            boolean isOverdue = isOverdue(rental, todayCal.getTime());
            boolean isDoneOnTargetDate = checkIsDoneOnTarget(rental, targetDate);

            boolean isExpiringSoon = false;
            long daysLeft = 99;

            if (isRentActive && rental.getEndDate() != null) {
                long endMillis = rental.getEndDate().toDate().getTime();
                long diffMillis = endMillis - todayCal.getTimeInMillis();
                daysLeft = diffMillis / (1000 * 60 * 60 * 24);
                if (daysLeft >= 0 && daysLeft <= 3) isExpiringSoon = true;
            }

            // PERBAIKAN FATAL: Alert Global (Retrieval, Complaint, Expiring, Overdue)
            // HANYA AKAN MUNCUL JIKA USER MELIHAT KALENDER HARI INI (isTargetToday).
            if ((isRetrieval && isTargetToday) ||
                    (isComplaint && isTargetToday) ||
                    (isExpiringSoon && isTargetToday && !isComplaint) ||
                    (isMaintDay && !isDoneOnTargetDate) ||
                    (isOverdue && isTargetToday && !isDoneOnTargetDate) ||
                    isDoneOnTargetDate) {

                MaintenanceTaskUIModel uiModel = buildUIModel(rental, isComplaint, isOverdue, targetDate, isDoneOnTargetDate, false);

                if (isRetrieval) {
                    uiModel.taskStatusText = "KONTRAK HABIS: TARIK TANAMAN!";
                    uiModel.statusColorCode = com.example.florist.R.color.purple_500; // <-- UBAH KE SINI
                    uiModel.displayNextDate = "Lakukan penarikan secepatnya";
                    uiModel.showCompleteButton = true;
                    uiModel.buttonText = "Kirim Bukti Penarikan";
                    uiModel.isComplaintVisit = false;
                } else if (isExpiringSoon && !isComplaint && !isRetrieval) {
                    uiModel.taskStatusText = "HAMPIR HABIS";
                    uiModel.statusColorCode = com.example.florist.R.color.blue_300; // <-- UBAH KE SINI
                    uiModel.displayNextDate = "Sisa Waktu: " + daysLeft + " Hari";
                    uiModel.showCompleteButton = true;
                    uiModel.buttonText = "Tawarkan via Chat";
                    uiModel.isComplaintVisit = false;
                } else {
                    uiModel.activeComplaintId = activeComplaintId;
                }
                filteredList.add(uiModel);
            }
        }
        filteredTasks.setValue(filteredList);
    }

    private boolean checkIsDoneOnTarget(Rental rental, Date targetDate) {
        if (rental.getLastMaintenanceDate() == null) return false;
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);
        Calendar lastMaintCal = Calendar.getInstance();
        lastMaintCal.setTime(rental.getLastMaintenanceDate().toDate());
        return lastMaintCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) &&
                lastMaintCal.get(Calendar.DAY_OF_YEAR) == targetCal.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isMaintenanceDay(Rental rental, Date targetDate) {
        if (rental.getStartDate() == null) return false;
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(targetDate);
        targetCal.set(Calendar.HOUR_OF_DAY, 0); targetCal.set(Calendar.MINUTE, 0); targetCal.set(Calendar.SECOND, 0); targetCal.set(Calendar.MILLISECOND, 0);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(rental.getStartDate().toDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);

        if (targetCal.before(startCal)) return false;

        if (rental.getEndDate() != null) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(rental.getEndDate().toDate());
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
            if (targetCal.after(endCal)) return false;
        }

        long diffMillis = targetCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
        return (diffDays % 3) == 0;
    }

    private boolean isOverdue(Rental rental, Date today) {
        if (rental.getStartDate() == null) return false;

        Calendar todayCal = Calendar.getInstance();
        todayCal.setTime(today);

        Calendar startCal = Calendar.getInstance();
        startCal.setTime(rental.getStartDate().toDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0); startCal.set(Calendar.MINUTE, 0); startCal.set(Calendar.SECOND, 0); startCal.set(Calendar.MILLISECOND, 0);

        if (todayCal.before(startCal)) return false;

        if (rental.getEndDate() != null) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(rental.getEndDate().toDate());
            endCal.set(Calendar.HOUR_OF_DAY, 23); endCal.set(Calendar.MINUTE, 59); endCal.set(Calendar.SECOND, 59);
            if (todayCal.after(endCal)) {
                return false;
            }
        }

        long diffMillis = todayCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffDays = diffMillis / (1000 * 60 * 60 * 24);
        long lastScheduledDay = diffDays - (diffDays % 3);

        if (lastScheduledDay <= 0) return false;

        Calendar lastScheduledCal = (Calendar) startCal.clone();
        lastScheduledCal.add(Calendar.DAY_OF_YEAR, (int) lastScheduledDay);

        Calendar actualLastMaintCal = Calendar.getInstance();
        if (rental.getLastMaintenanceDate() != null) {
            actualLastMaintCal.setTime(rental.getLastMaintenanceDate().toDate());
        } else {
            actualLastMaintCal.setTime(rental.getStartDate().toDate());
        }
        actualLastMaintCal.set(Calendar.HOUR_OF_DAY, 0); actualLastMaintCal.set(Calendar.MINUTE, 0); actualLastMaintCal.set(Calendar.SECOND, 0); actualLastMaintCal.set(Calendar.MILLISECOND, 0);

        return actualLastMaintCal.before(lastScheduledCal);
    }

    public void AddMaintenance(Rental rental, Uri imageUri, String description, String activeComplaintId) {
        isLoading.setValue(true);
        repository.submitMaintenanceLog(rental, imageUri, description, activeComplaintId, new MaintenanceRepository.ActionCallback() {
            @Override
            public void onSuccess() {
                isLoading.setValue(false);
                actionSuccessMessage.setValue("Laporan berhasil dikirim!");
            }

            @Override
            public void onError(String message) {
                isLoading.setValue(false);
                errorMessage.setValue(message);
            }
        });
    }

    private MaintenanceTaskUIModel buildUIModel(Rental rental, boolean isComplaint, boolean isOverdue, Date targetDate, boolean isDoneOnTargetDate, boolean isWaitingConfirmation) {
        MaintenanceTaskUIModel model = new MaintenanceTaskUIModel();
        model.rental = rental;

        String shortId = rental.getRentalId();
        if (shortId != null && shortId.length() > 8) {
            shortId = shortId.substring(0, 8) + "...";
        }
        model.displayOrderId = "Sewa ID: " + shortId;

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault());
        String startStr = rental.getStartDate() != null ? sdf.format(rental.getStartDate().toDate()) : "-";
        String endStr = rental.getEndDate() != null ? sdf.format(rental.getEndDate().toDate()) : "-";
        model.displayDuration = "Masa Sewa: " + startStr + " s/d " + endStr;

        if (isDoneOnTargetDate) {
            if (isWaitingConfirmation) {
                model.taskStatusText = "Menunggu Konfirmasi Pembeli";
            } else {
                model.taskStatusText = "Tugas Selesai Dikerjakan";
                model.statusColorCode = com.example.florist.R.color.olive_500;
            }
            model.displayNextDate = "Diselesaikan pada: " + sdf.format(targetDate);
            model.showCompleteButton = false;
            model.isComplaintVisit = false;
        }
        else if ("SELESAI".equalsIgnoreCase(rental.getStatus())) {
            model.taskStatusText = "Sewa Berakhir";
            model.statusColorCode = com.example.florist.R.color.gray_500; // <-- UBAH KE SINI
            model.showCompleteButton = false;
            model.displayNextDate = "-";
        }
        else if (isComplaint) {
            model.taskStatusText = "TUGAS: PERBAIKAN KOMPLAIN!";
            model.statusColorCode = com.example.florist.R.color.red_500; // <-- UBAH KE SINI
            model.displayNextDate = "SEGERA LAKUKAN KUNJUNGAN";
            model.showCompleteButton = true;
            model.buttonText = "Kirim Bukti Perbaikan";
            model.isComplaintVisit = true;
        }
        else if (isOverdue) {
            model.taskStatusText = "JADWAL TERLEWAT!";
            model.statusColorCode = com.example.florist.R.color.yellow_600; // <-- UBAH KE SINI
            model.displayNextDate = "Tugas Tertunda yang Belum Diselesaikan";
            model.showCompleteButton = true;
            model.buttonText = "Kirim Bukti Kunjungan";
            model.isComplaintVisit = false;
        }
        else {
            model.taskStatusText = "Tugas Perawatan Rutin";
            model.statusColorCode = com.example.florist.R.color.green_500; // <-- UBAH KE SINI
            model.showCompleteButton = true;
            model.buttonText = "Kirim Bukti Perawatan";
            model.isComplaintVisit = false;
            model.displayNextDate = "Jadwal: " + sdf.format(targetDate);
        }
        return model;
    }

    private void generateCalendarIndicators() {
        List<Rental> rentals = allRentals.getValue();
        if (rentals == null) return;

        CalendarIndicators indicators = new CalendarIndicators();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());

        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 14; i++) {
            Date dateToCheck = cal.getTime();
            String dateStr = sdf.format(dateToCheck);

            for (Rental rental : rentals) {
                String activeComplaintId = rental.getActiveComplaintId();

                if (activeComplaintId != null && !activeComplaintId.trim().isEmpty()) {
                    if (!indicators.complaintDates.contains(dateStr)) indicators.complaintDates.add(dateStr);

                } else if (isMaintenanceDay(rental, dateToCheck)) {
                    if (!checkIsDoneOnTarget(rental, dateToCheck)) {
                        if (!indicators.routineDates.contains(dateStr)) indicators.routineDates.add(dateStr);
                    }
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        calendarIndicators.setValue(indicators);
    }
}