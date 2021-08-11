package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.FloatRange;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public class ImpactCounterImpl implements ImpactCounter {
    //    private List<Measurement> notProceededMeasurements;
    private Measurement previousMeasurement;
    private Measurement lastImpactMeasurement;
    private long minTimeIntervalBetweenImpactMillis;
    private double minDifference;

    @Override
    public int findImpacts(@NonNull List<Measurement> measurementsToSearch,
                           @IntRange(from = 5, to = 20) int measurementWindow,
                           @IntRange(from = 50, to = 2000) long minTimeIntervalBetweenImpactMillis,
                           @FloatRange(from = 0.1, to = 2.0) double minDifference) {
        int impacts = 0;
        this.minDifference = minDifference;
        this.minTimeIntervalBetweenImpactMillis = minTimeIntervalBetweenImpactMillis;
//        if (notProceededMeasurements == null) {
//            notProceededMeasurements = new ArrayList<>(measurementsToSearch);
//        } else {
//            notProceededMeasurements.addAll(measurementsToSearch);
//        }
//        if (notProceededMeasurements.size() < measurementWindow) {
//            return impacts;
//        } else {
        for (Measurement currentMeasurement : measurementsToSearch) {
            if (previousMeasurement == null) {
                previousMeasurement = currentMeasurement;
            } else {
                boolean isMinTimeIntervalPreserved = checkMinTimeInterval(currentMeasurement);
                boolean isMinDifferencePreserved = checkMinDifference(currentMeasurement);
                if (isMinDifferencePreserved && isMinTimeIntervalPreserved) {
                    lastImpactMeasurement = currentMeasurement;
                    impacts++;
                }
            }
        }
//        }
        return impacts;
    }

    private boolean checkMinDifference(Measurement currentMeasurement) {
        double currentDistance = currentMeasurement.getDistanceCentimeters();
        double previousDistance = previousMeasurement.getDistanceCentimeters();
        return previousDistance - currentDistance >= minDifference;
    }

    private boolean checkMinTimeInterval(Measurement currentMeasurement) {
        long currentTimestamp = currentMeasurement.getDate().getTime();
        long lastImpactTimestamp;
        if (lastImpactMeasurement != null) {
            lastImpactTimestamp = lastImpactMeasurement.getDate().getTime();
        } else {
            return true;
        }
        return currentTimestamp - lastImpactTimestamp >= minTimeIntervalBetweenImpactMillis;
    }

    //    private boolean isImpactFound() {
//        if (recordedSensorData.size() > maxDifference) {
//            double sum = 0;
//            for (int i = filteredSensorData.size() - maxDifference - 1; i < filteredSensorData.size() - 1; i++) {
//                sum += recordedSensorData.get(i).getDistanceCentimeters();
//            }
//            double averageFromPreviousXMeasurements = sum / maxDifference;
//            double differenceToCheck = averageFromPreviousXMeasurements - recordedSensorData.get(recordedSensorData.size() - 1).getDistanceCentimeters();
//            if (differenceToCheck > minDifference) {
//                long currentMillis = System.currentTimeMillis();
//                long timeDifference = currentMillis - previousImpactTimestamp;
//                if (timeDifference >= minTimeIntervalBetweenImpactMillis) {
//                    impacts++;
//                    previousImpactTimestamp = currentMillis;
//                    return true;
//                }
//            }
//        }
//        return false;
//    }
}
