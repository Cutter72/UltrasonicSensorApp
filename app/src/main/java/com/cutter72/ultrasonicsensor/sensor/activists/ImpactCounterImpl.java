package com.cutter72.ultrasonicsensor.sensor.activists;

import androidx.annotation.NonNull;

import com.cutter72.ultrasonicsensor.sensor.solids.Measurement;

import java.util.List;

public class ImpactCounterImpl implements ImpactCounter {
    @Override
    public int findImpacts(@NonNull List<Measurement> measurementsToSearch) {

        return 0;
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
